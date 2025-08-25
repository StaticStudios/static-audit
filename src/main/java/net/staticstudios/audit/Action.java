package net.staticstudios.audit;

import com.google.gson.Gson;

import java.util.Objects;

/**
 * Represents an action that can be logged in the audit system.
 */
public interface Action<T extends ActionData> {
    Gson GSON_INSTANCE = new Gson();

    /**
     * Create a simple action with the given ID and data class.
     *
     * @param actionId        the unique identifier for the action
     * @param actionDataClass the class of the action data
     * @return a new Action instance
     */
    static <T extends ActionData> Action<T> simple(String actionId, Class<T> actionDataClass) {
        return new SimpleAction<>(actionId, actionDataClass);
    }

    /**
     * Get the unique identifier for this action.
     *
     * @return the action ID
     */
    String getActionId();

    /**
     * Convert the given action data to a JSON string.
     *
     * @param data the action data to convert
     * @return the JSON representation of the action data
     */
    String toJson(T data);

    /**
     * Convert the given JSON string to action data.
     *
     * @param json the JSON string to convert
     * @return the action data
     */
    T fromJson(String json);

    /**
     * Get the class of the action data.
     *
     * @return the action data class
     */
    Class<T> getDataType();

    /**
     * A simple implementation of the Action interface.
     */
    class SimpleAction<T extends ActionData> implements Action<T> {
        private final String actionId;
        private final Class<T> actionDataClass;

        public SimpleAction(String actionId, Class<T> actionDataClass) {
            this.actionId = actionId;
            this.actionDataClass = actionDataClass;
        }

        @Override
        public String getActionId() {
            return actionId;
        }

        @Override
        public String toJson(T data) {
            return GSON_INSTANCE.toJson(data);
        }

        @Override
        public T fromJson(String json) {
            return GSON_INSTANCE.fromJson(json, actionDataClass);
        }

        @Override
        public Class<T> getDataType() {
            return actionDataClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SimpleAction<?> that)) return false;
            return Objects.equals(actionId, that.actionId) && Objects.equals(actionDataClass, that.actionDataClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(actionId, actionDataClass);
        }
    }
}
