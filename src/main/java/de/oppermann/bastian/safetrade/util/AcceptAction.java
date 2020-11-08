package de.oppermann.bastian.safetrade.util;

/**
 * This interface is required for the {@link AcceptCommandManager}.
 */
public interface AcceptAction {

    /**
     * This is called when the player accepted.
     */
    void perform();

    /**
     * This is called when the action times out.
     */
    void onTimeout();

}
