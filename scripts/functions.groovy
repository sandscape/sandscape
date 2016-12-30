//functions used by sandscape.groovy and Sandscape plugins

getObjectValue = null
getObjectValue = { Map object, String key, Object defaultValue ->
    if(key.indexOf('.') >= 0) {
        String key1 = key.split('\\.', 2)[0]
        String key2 = key.split('\\.', 2)[1]
        if(object.get(key1) != null && object.get(key1) instanceof Map) {
            return getObjectValue(object.get(key1), key2, defaultValue)
        }
        else {
            return defaultValue
        }
    }

    //try returning the value casted as the same type as defaultValue
    try {
        if(object.get(key) != null) {
            if((defaultValue instanceof String) && ((object.get(key) instanceof Map) || (object.get(key) instanceof List))) {
                return defaultValue
            }
            else {
                if((defaultValue instanceof Boolean) && (object.get(key) == 'false')) {
                    return false
                }
                else {
                    return object.get(key).asType(defaultValue.getClass())
                }
            }
        }
    }
    catch(Exception e) {}

    //nothing worked so just return default value
    return defaultValue
}
