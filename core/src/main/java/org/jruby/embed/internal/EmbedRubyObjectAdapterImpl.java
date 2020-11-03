/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import org.jruby.Ruby;
import org.jruby.RubyInteger;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyObjectAdapter;
import org.jruby.RubyString;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EmbedRubyObjectAdapter;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.variable.BiVariable;
import org.jruby.embed.variable.InstanceVariable;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaObject;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

import static org.jruby.embed.internal.EmbedEvalUnitImpl.isSharingVariables;

/**
 * Implementation of {@link EmbedRubyObjectAdapter}. Users get an instance of this
 * class by newObjectAdapter() method of {@link ScriptingContainer}.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class EmbedRubyObjectAdapterImpl implements EmbedRubyObjectAdapter {

    private final RubyObjectAdapter adapter = JavaEmbedUtils.newObjectAdapter();
    private final ScriptingContainer container;

    public enum MethodType {
        CALLMETHOD_NOARG,
        CALLMETHOD,
        CALLMETHOD_WITHBLOCK,
        CALLSUPER,
        CALLSUPER_WITHBLOCK
    }

    public EmbedRubyObjectAdapterImpl(ScriptingContainer container) {
        this.container = container;
    }

    public boolean isKindOf(IRubyObject value, RubyModule rubyModule) {
        return adapter.isKindOf(value, rubyModule);
    }

    public IRubyObject[] convertToJavaArray(IRubyObject array) {
        return adapter.convertToJavaArray(array);
    }

    public RubyInteger convertToRubyInteger(IRubyObject obj) {
        return adapter.convertToRubyInteger(obj);
    }

    public RubyString convertToRubyString(IRubyObject obj) {
        return adapter.convertToRubyString(obj);
    }

    public IRubyObject setInstanceVariable(IRubyObject obj, String variableName, IRubyObject value) {
        BiVariableMap map = container.getVarMap();
        synchronized (map) {
            if (map.containsKey(variableName)) {
                BiVariable bv = map.getVariable((RubyObject)container.getProvider().getRuntime().getTopSelf(), variableName);
                bv.setRubyObject(value);
            } else {
                InstanceVariable iv = new InstanceVariable(obj, variableName, value);
                map.update(variableName, iv);
            }
        }
        return obj.getInstanceVariables().setInstanceVariable(variableName, value);
    }

    public IRubyObject getInstanceVariable(IRubyObject obj, String variableName) {
        BiVariableMap map = container.getVarMap();
        synchronized (map) {
            if (map.containsKey(variableName)) {
                BiVariable bv = map.getVariable((RubyObject)container.getProvider().getRuntime().getTopSelf(), variableName);
                return bv.getRubyObject();
            }
        }
        return null;
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName) {
        return adapter.callMethod(receiver, methodName);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject singleArg) {
        return adapter.callMethod(receiver, methodName, singleArg);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args) {
        return adapter.callMethod(receiver, methodName, args);
    }

    public IRubyObject callMethod(IRubyObject receiver, String methodName, IRubyObject[] args, Block block) {
        return adapter.callMethod(receiver, methodName, args, block);
    }

    public IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args) {
        return adapter.callSuper(receiver, args);
    }

    public IRubyObject callSuper(IRubyObject receiver, IRubyObject[] args, Block block) {
        return adapter.callSuper(receiver, args, block);
    }

    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType) {
        try {
            return call(MethodType.CALLMETHOD_NOARG, returnType, getReceiverObject(receiver), methodName, null, null);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Object singleArg, Class<T> returnType) {
        try {
            return call(MethodType.CALLMETHOD, returnType, getReceiverObject(receiver), methodName, null, null, singleArg);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType) {
        try {
            return call(MethodType.CALLMETHOD, returnType, getReceiverObject(receiver), methodName, null, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType) {
        try {
            return call(MethodType.CALLMETHOD_WITHBLOCK, returnType, getReceiverObject(receiver), methodName, block, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Class<T> returnType, EmbedEvalUnit unit) {
        try {
            return call(MethodType.CALLMETHOD_NOARG, returnType, getReceiverObject(receiver), methodName, null, unit);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType, EmbedEvalUnit unit) {
        try {
            return call(MethodType.CALLMETHOD, returnType, getReceiverObject(receiver), methodName, null, unit, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType, EmbedEvalUnit unit) {
        try {
            return call(MethodType.CALLMETHOD_WITHBLOCK, returnType, getReceiverObject(receiver), methodName, block, unit, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callSuper(Object receiver, Object[] args, Class<T> returnType) {
        try {
            return call(MethodType.CALLSUPER, returnType, getReceiverObject(receiver), null, null, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType) {
        try {
            return call(MethodType.CALLSUPER_WITHBLOCK, returnType, getReceiverObject(receiver), null, block, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public Object callMethod(Object receiver, String methodName, Object... args) {
        try {
            if (args.length == 0) {
                return call(MethodType.CALLMETHOD_NOARG, Object.class, getReceiverObject(receiver), methodName, null, null);
            } else {
                return call(MethodType.CALLMETHOD, Object.class, getReceiverObject(receiver), methodName, null, null, args);
            }
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public Object callMethod(Object receiver, String methodName, Block block, Object... args) {
        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("needs at least one argument in a method");
            }
            return call(MethodType.CALLMETHOD_WITHBLOCK, Object.class, getReceiverObject(receiver), methodName, block, null, args);
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    public <T> T runRubyMethod(Class<T> returnType, Object receiver, String methodName, Block block, Object... args) {
        try {
            RubyObject rubyReceiver = (RubyObject)JavaEmbedUtils.javaToRuby(container.getProvider().getRuntime(), receiver);
            if (args.length == 0) {
                return call(MethodType.CALLMETHOD_NOARG, returnType, rubyReceiver, methodName, block, null);
            } else {
                return call(MethodType.CALLMETHOD, returnType, rubyReceiver, methodName, block, null, args);
            }
        } catch (InvokeFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        }
    }

    private <T> T call(MethodType type, Class<T> returnType, IRubyObject rubyReceiver, String methodName, Block block, EmbedEvalUnit unit, Object... args) {
        final Ruby runtime = container.getProvider().getRuntime();
        final boolean sharing_variables = isSharingVariables(container);

        if (sharing_variables) {
            ManyVarsDynamicScope scope;
            if (unit != null && unit.getScope() != null) scope = unit.getScope();
            else scope = EmbedRubyRuntimeAdapterImpl.getManyVarsDynamicScope(container, 0);
            container.getVarMap().inject(scope);
            runtime.getCurrentContext().pushScope(scope);
        }

        try {
            IRubyObject result = callImpl(runtime, type, rubyReceiver, methodName, block, args);
            if (sharing_variables) {
                container.getVarMap().retrieve(rubyReceiver);
            }
            if (returnType != null) {
                Object ret = JavaEmbedUtils.rubyToJava(runtime, result, returnType);
                return returnType.cast(ret);
            }
            return null;
        } catch (RaiseException e) {
            runtime.printError(e.getException());
            throw new InvokeFailedException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new InvokeFailedException(e);
        } finally {
            if (sharing_variables) {
                runtime.getCurrentContext().popScope();
            }
        }
    }

    private IRubyObject getReceiverObject(Object receiver) {
        if (receiver instanceof IRubyObject) return (IRubyObject) receiver;
        return container.getProvider().getRuntime().getTopSelf();
    }

    private static IRubyObject callImpl(final Ruby runtime,
        MethodType type, IRubyObject rubyReceiver, String methodName, Block block, Object... args) {
        IRubyObject[] rubyArgs;
        if (args.length > 0) {
            rubyArgs = JavaUtil.convertJavaArrayToRuby(runtime, args);
            for (int i = 0; i < rubyArgs.length; i++) {
                IRubyObject obj = rubyArgs[i];
                if (obj instanceof JavaObject) {
                    rubyArgs[i] = Java.wrap(runtime, obj);
                }
            }
        } else {
            rubyArgs = IRubyObject.NULL_ARRAY;
        }
        ThreadContext context = runtime.getCurrentContext();
        switch (type) {
            case CALLMETHOD_NOARG:
                return Helpers.invoke(context, rubyReceiver, methodName);
            case CALLMETHOD:
                return Helpers.invoke(context, rubyReceiver, methodName, rubyArgs);
            case CALLMETHOD_WITHBLOCK:
                return Helpers.invoke(context, rubyReceiver, methodName, rubyArgs, block);
            case CALLSUPER:
                return Helpers.invokeSuper(context, rubyReceiver, rubyArgs, Block.NULL_BLOCK);
            case CALLSUPER_WITHBLOCK:
                return Helpers.invokeSuper(context, rubyReceiver, rubyArgs, block);
            default:
                throw new AssertionError("unexpected method type");
        }
    }
}
