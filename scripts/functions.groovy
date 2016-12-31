//functions used by sandscape.groovy and Sandscape plugins

import java.text.DateFormat
import java.util.Date
import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.lang.exception.ExceptionUtils

/*
   Define available bindings.  The bindings had to be listed first in order for
   recursion to work within closures.
*/
downloadFile = null
getObjectValue = null
isUrlGood = null
logger = null
sandscapeErrorLevelLogger = null
sandscapeErrorLogger = null
sandscapeLevelLogger = null
sandscapeLogger = null
sandscapePluginErrorLogger = null
sandscapePluginLevelLogger = null
sandscapePluginLogger = null

/*
Sandscape logging mechanisms which are used by scripts and Sandscape plugins.

USAGE:

For sandscape scripts,

    sandscapeLogger("This is an informational message.")
    sandscapeErrorLogger("A critical error has occurred.")

For sandscape plugins,

    sandscapePluginLogger("This is an informational message.")
    sandscapePluginErrorLogger("A critical error has occurred.")
*/
//sandscape logging mechanisms
logger = Logger.getLogger('sandscape')
sandscapeLevelLogger = { Level level, String message ->
    String now = DateFormat.getDateTimeInstance().format(new Date())
    "${now} ${message}".with {
        if(level == Level.SEVERE) {
            println it
        }
        logger.log(level, it)
    }
}
sandscapePluginLevelLogger = {Level level, String message ->
    //get the class name of the class which called this function
    String callerClass = Thread.currentThread().getStackTrace().findAll { it.getFileName() && it.getFileName().endsWith('.groovy') }[1].with { it = it.getFileName() - '.groovy' }
    sandscapeLevelLogger(level, "[${callerClass} sandscape plugin] ${message}")
}
//http://groovy-lang.org/closures.html#_left_currying
sandscapeErrorLogger = sandscapeLevelLogger.curry(Level.SEVERE)
sandscapeLogger = sandscapeLevelLogger.curry(Level.INFO)
sandscapePluginErrorLogger = sandscapePluginLevelLogger.curry(Level.SEVERE)
sandscapePluginLogger = sandscapePluginLevelLogger.curry(Level.INFO)

/*
Get an object from a `Map` or return any object from `defaultValue`.
Guarantees that what is returned is the same type as `defaultValue`.  This is
used to get optional keys from YAML or JSON files.

PARAMETERS:

* `object` - A `Map` which was likely created from a YAML or JSON file.
* `key` - A `String` with keys and subkeys separated by periods which is used
  to search the `object` for a possible value.
* `defaultValue` - A default value and type that should be returned.

RETURNS:

Returns the value of the key or a `defaultValue` which is of the same type as
`defaultValue`.  This function has coercion behaviors which are not the same as
Groovy:

* If the `defaultValue` is an instance of `String` and the retrieved key is an
  instance of `Map`, then `defaultValue` is returned rather than converting it
  to a `String`.
* If the `defaultValue` is an instance of `String` and the retrieved key is an
  instance of `List`, then `defaultValue` is returned rather than converting it
  to a `String`.
* If the `defaultValue` is an instance of `Boolean`, the retrieved key is an
  instance of `String` and has a value of `false`, then `Boolean false` is
  returned.
*/
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

/*
    This function tests the health of a URL HTTP status by calling the HTTP HEAD
    method of the HTTP protocol.

    USAGE:



    PARAMETERS:

    * `url` - A `String` which is the URL of a website.

    RETURNS:

    A `Boolean`, `true` if the website returns an HTTP 2XX status code and
    `false` otherwise.
*/
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

/*
   Download a file to a local `fullpath`.  If the parent directories of the
   path are missing then they are automatically created.
*/
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
