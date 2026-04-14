package utility.prefabrication;

/**
 * PrefabData is a wrapper for a json object source used in instantiation.
 * @param name name of the prefab
 * @param json the JSON string contain the prefab data
 * @param description the description of the prefab
 */
public record PrefabData(String name, String json, String description) {
}
