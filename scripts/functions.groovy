//functions used by sandscape.groovy and Sandscape plugins

import java.text.DateFormat
import java.util.Date
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.lang.exception.ExceptionUtils

//list of available bindings
downloadFile = null
getObjectValue = null
isUrlGood = null
sandscapeErrorLevelLogger = null
sandscapeErrorLogger = null
sandscapeLevelLogger = null
sandscapeLogger = null
sandscapePluginErrorLogger = null
sandscapePluginLevelLogger = null
sandscapePluginLogger = null

/*
   Define the bindings as functions.  The bindings had to be listed first in
   order for recursion to work within closures.
*/

//sandscape logging mechanisms
logger = Logger.getLogger('sandscape')
sandscapeLevelLogger = { Level level, String message ->
    String now = DateFormat.getDateTimeInstance().format(new Date())
    logger.log(level, "${now} ${message}")
}
sandscapeErrorLevelLogger = { Level level, String message ->
    String now = DateFormat.getDateTimeInstance().format(new Date())
    "${now} ${message}".with {
        println it
        logger.log(level, it)
    }
}
sandscapeLogger = { String message ->
    sandscapeLevelLogger(Level.INFO, message)
}
sandscapeErrorLogger = { String message ->
    sandscapeErrorLevelLogger(Level.SEVERE, message)
}
sandscapePluginLogger = { Class clazz, String message ->
    sandscapeLevelLogger(Level.INFO, "[${clazz.simpleName} sandscape plugin] ${message}")
}
sandscapePluginErrorLogger = { Class clazz, String message ->
    sandscapeErrorLevelLogger(Level.SEVERE, "[${clazz.simpleName} sandscape plugin] ${message}")
}
sandscapePluginLevelLogger = { Class clazz, Level level, String message ->
    sandscapeLevelLogger(level, "[${clazz.simpleName} sandscape plugin] ${message}")
}

//a function for easily traversing objects
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

isUrlGood = { String url ->
    int code = -1
    try {
        code = new URL(url).openConnection().with {
            requestMethod = 'HEAD'
            //override HTTP headers Java sends for security reasons (discovered via tcpdump to http://example.com)
            addRequestProperty "User-Agent", "Java"
            addRequestProperty "Connection", "close"
            addRequestProperty "Accept", "*/*"
            //no network connection has been made up until this point
            responseCode
            //network connection is immediately closed
        }
    }
    catch(MalformedURLException e) {
        sandscapeErrorLogger(ExceptionUtils.getStackTrace(e))
    }
    catch(Exception e) {}
    //2XX status is success - https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2
    //return true if HTTP 2XX status
    return ((int) code/100) == 2
}

downloadFile = { String url, String fullpath ->
    try {
        new File(fullpath).with { file ->
            //make parent directories if they don't exist
            if(!file.getParentFile().exists()) {
                file.getParentFile().mkdirs()
            }
            file.newOutputStream().with { file_os ->
                file_os << new URL(url).openStream()
                file_os.close()
            }
        }
    }
    catch(Exception e) {
        sandscapeErrorLogger(ExceptionUtils.getStackTrace(e))
        return false
    }
    return true
}
