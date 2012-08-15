package org.jruby.ir.persistence.read;

import java.io.InputStream;

import org.jruby.Ruby;
import org.jruby.ir.IRScope;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.read.lexer.PersistedIRScanner;
import org.jruby.ir.persistence.read.parser.IRParsingContext;
import org.jruby.ir.persistence.read.parser.PersistedIRParser;

public class IRReader {

    private static final PersistedIRParser parser = new PersistedIRParser();

    public static IRScope read(InputStream is, Ruby runtime) throws IRPersistenceException {
        try {
            final PersistedIRScanner input = new PersistedIRScanner(is);
            final IRParsingContext context = new IRParsingContext(runtime);
            
            parser.init(context);
            return (IRScope) parser.parse(input);
        } catch (Exception e) {
            throw new IRPersistenceException(e);
        }
    }

}
