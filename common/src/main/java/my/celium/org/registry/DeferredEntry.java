package my.celium.org.registry;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy handle for one registered object. Behaves like a memoised
 * {@link Supplier}: {@link #get()} returns the registered value once the
 * loader has created it.
 */
public final class DeferredEntry<T> implements Supplier<T> {
    private final Supplier<T> delegate;

    DeferredEntry(Supplier<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public T get() {
        return delegate.get();
    }

    /** Whether the underlying object exists yet. Never throws. */
    public boolean isPresent() {
        try {
            return get() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
