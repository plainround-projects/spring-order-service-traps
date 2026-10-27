package dev.plainround.orders.domain;

/**
 * A customer who places orders.
 *
 * @param id the identifier assigned by the store
 * @param name the display name, 1 to {@value #MAX_NAME_LENGTH} characters, not blank
 */
public record Customer(long id, String name) {

    /** Longest accepted name. */
    public static final int MAX_NAME_LENGTH = 100;

    /** Checks the invariants. */
    public Customer {
        requireName(name);
    }

    /**
     * Checks a name before the customer is stored, when there is no id yet.
     *
     * @param name the candidate name
     * @throws InvalidCustomer if the name is blank or too long
     */
    public static void requireName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidCustomer("name must be 1 to " + MAX_NAME_LENGTH + " characters");
        }
    }
}
