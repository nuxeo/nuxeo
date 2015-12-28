package org.nuxeo.runtime.api;

public class LambdaAdaptor {
	public interface RunnableCheckException<X extends Exception> {
		void run() throws X;
	}

	public interface Consumer<T> {
		void accept(T value);
	}

	public interface Supplier<T> {
		T get();
	}

	public interface Function<T, R> {
		R apply(T value);
	}

	public static class Optional<T> {

		static final Optional<?> empty = new Optional<>();

		static <T> Optional<T> of(T value) {
			return new Optional<T>(value);
		}

		@SuppressWarnings("unchecked")
		static <T> Optional<T> empty() {
			return (Optional<T>) empty;
		}

		static <T> Optional<T> ofNullable(T value) {
			return value == null ? Optional.<T>empty() : of(value);
		}

		final T value;

		Optional() {
			value = null;
		}

		Optional(T value) {
			this.value = value;
		}

		<R> Optional<R> map(Function<T, R> function) {
			return value == null ? Optional.<R>empty() : of(function.apply(value));
		}

		T orElseGet(Supplier<T> supplier) {
			return value != null ? value : supplier.get();
		}

		T orElse(T otherValue) {
			return value != null ? value : otherValue;
		}

		void ifPresent(Consumer<T> consumer) {
			if (value != null) {
				consumer.accept(value);
			}
		}

		boolean isPresent() {
			return value != null;
		}
	}
}