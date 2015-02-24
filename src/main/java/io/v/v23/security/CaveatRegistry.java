package io.v.v23.security;

import io.v.v23.uniqueid.Id;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * CaveatRegistry implements a singleton global registry that maps the unique id of a caveat to its
 * validator.
 *
 * It is safe to invoke methods on CaveatRegistry concurrently.
 */
public class CaveatRegistry {
    private static final Map<Id, RegistryEntry> validators = new HashMap<Id, RegistryEntry>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Associates a the caveat descriptor with the validator that is used for validating
     * all caveats with the same identifier as the descriptor.
     * This method may be called at most once per unique identifier and will throw an exception
     * on duplicate registrations.
     *
     * @param  desc       caveat descriptor
     * @param  validator  caveat validator
     * @throws VException      if the given caveat validator and descriptor couldn't be registered
     */
    public static void register(CaveatDescriptor desc, CaveatValidator validator)
            throws VException {
        final String registerer = getRegisterer();
        lock.writeLock().lock();
        final RegistryEntry existing = validators.get(desc.getId());
        if (existing != null) {
            lock.writeLock().unlock();
            throw new VException(String.format("Caveat with UUID %s registered twice. " +
                "Once with (%s, validator=%s) from %s, once with (%s, validator=%s) from %s",
                desc.getId(), existing.getDescriptor().getParamType(), existing.getValidator(),
                existing.getRegisterer(), desc.getParamType(), validator, registerer));
        }
        // TODO(spetrovic): Once rogulenko@ is done, get the Type from desc.getParamType().
        final Type paramType = null;
        final RegistryEntry entry = new RegistryEntry(desc, validator, paramType, registerer);
        validators.put(desc.getId(), entry);
        lock.writeLock().unlock();
    }

    /**
     * Throws an exception iff the restriction encapsulated in the corresponding caveat
     * hasn't been satisfied by the provided context.
     *
     * @param  ctx        context matched against the caveat
     * @param  caveat     security caveat
     * @throws VException      if the caveat couldn't be validated
     */
    public static void validate(VContext ctx, Caveat caveat) throws VException {
        final RegistryEntry entry = lookup(caveat.getId());
        if (entry == null) {
            throw Errors.makeCaveatNotRegistered(null, caveat.getId());
        }
        Object param = null;
        try {
            // TODO(spetrovic): Once rogulenko@ is done, decode with entry.getParamType().
            param = VomUtil.decode(caveat.getParamVom());
        } catch (VException e) {
            throw new VException(e.getMessage());
        }
        // TODO(spetrovic): Once rogulenko@ is done, pass the type as well.
        entry.validator.validate(ctx, param);
    }

    private static RegistryEntry lookup(Id id) {
        lock.readLock().lock();
        final RegistryEntry entry = validators.get(id);
        lock.readLock().unlock();
        return entry;
    }

    private static String getRegisterer() {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack == null || stack.length < 2) {
            return "";
        }
        final StackTraceElement registerer = stack[stack.length - 2];
        return String.format("%s:%d", registerer.getFileName(), registerer.getLineNumber());
    }

    private static class RegistryEntry {
        CaveatDescriptor desc;
        CaveatValidator validator;
        Type paramType;
        String registerer;

        RegistryEntry(CaveatDescriptor desc,
                CaveatValidator validator, Type paramType, String registerer) {
            this.desc = desc;
            this.validator = validator;
            this.paramType = paramType;
            this.registerer = registerer;
        }
        CaveatDescriptor getDescriptor() { return this.desc; }
        CaveatValidator getValidator() { return this.validator; }
        Type getParamType() { return this.paramType; }
        String getRegisterer() { return this.registerer; }
    }
}