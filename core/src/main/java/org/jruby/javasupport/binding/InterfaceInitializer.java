package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
final class InterfaceInitializer extends Initializer {

    InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyModule initialize(RubyModule proxy) {
        final State state = new State(runtime, null);

        Field[] fields = JavaClass.getDeclaredFields(javaClass);

        for (int i = fields.length; --i >= 0; ) {
            final Field field = fields[i];
            if ( javaClass != field.getDeclaringClass() ) continue;

            boolean isConstant = ConstantField.isConstant(field);
            if (isConstant) {
                state.constantFields.add(new ConstantField(field));
            }

            final int mod = field.getModifiers();
            if ( Modifier.isStatic(mod) ) {
                // If we already are adding it as a constant, make the accessors warn about deprecated behavior.
                // See jruby/jruby#5730.
                addField(state.getStaticInstallersForWrite(), state.staticNames, field, Modifier.isFinal(mod), true, isConstant);
            }
        }

        setupInterfaceMethods(javaClass, state);

        // Add in any Scala singleton methods
        handleScalaSingletons(javaClass, state);

        // Now add all aliases for the static methods (fields) as appropriate
        state.getStaticInstallers().forEach(($, installer) -> {
            if (installer.type == NamedInstaller.STATIC_METHOD && installer.hasLocalMethod()) {
                assignAliases((MethodInstaller) installer, state.staticNames);
            }
        });

        runtime.getJavaSupport().getStaticAssignedNames().get(javaClass).putAll(state.staticNames);
        runtime.getJavaSupport().getInstanceAssignedNames().get(javaClass).clear();

        installClassFields(proxy, state);
        installClassStaticMethods(proxy, state);
        installClassClasses(javaClass, proxy);

        proxy.getName(); // trigger calculateName()

        return proxy;
    }

    private static void setupInterfaceMethods(Class<?> javaClass, Initializer.State state) {
        getMethods(javaClass).forEach((name, methods) -> {
            for (int i = methods.size(); --i >= 0; ) {
                // Java 8 introduced static methods on interfaces, so we just look for those
                Method method = methods.get(i);

                if (!Modifier.isStatic(method.getModifiers())) continue;

                state.prepareStaticMethod(javaClass, method, name);
            }
        });

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases(state);
    }

}
