/*
Copyright (c) 2016 Sam Gleske - https://github.com/sandscape

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

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
setObjectValue = null

/*
Sandscape logging mechanisms which are used by scripts and Sandscape plugins.

USAGE:

For sandscape scripts,

    sandscapeLogger 'This is an informational message.'
    sandscapeErrorLogger 'A critical error has occurred.'

For sandscape plugins,

    sandscapePluginLogger 'This is an informational message.'
    sandscapePluginErrorLogger 'A critical error has occurred.'
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

USAGE:

    Map example = [key1: [subkey1: 'string']]
    getObjectValue(example, 'key1.subkey1', 'some default')

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
Write to an object which was loaded from YAML or JSON files.

USAGE:

Write `3` (an Integer) to the second item in a list.

    List someobject = ['a', 'b', 'c']
    setObjectValue(someobject, '[1]', 3)
    assert ['a', 3, 'c'] == someobject

In the third item, write a Map key value pair.

    List someobject = ['a', 'b', 'c']
    setObjectValue(someobject, '[2].hello', 'world')
    assert ['a', 'b', ['hello':'world']] == someobject

Write to a Map which contains a key whose contents is a list.  Write the same
value to every item in the list.

    Map someobject = ['rootkey': [1, 2, 3]]
    setObjectValue(someobject, 'rootkey[*]', 'groovy')
    assert ['rootkey': ['groovy', 'groovy', 'groovy']] == someobject

Write to a nested Map.

    Map someobject = ['people': [['name':'Jack'], ['name':'Jill']]]
    setObjectValue(someobject, 'people[0].age', 15)
    assert ['name': 'Jack', 'age': 15] == someobject['people'][0]

PARAMETERS:

* `object` - A `Map` or a `List` which was likely created from a YAML or JSON
  file.
* `key` - A `String` with keys and subkeys separated by periods which is used
  as a path to write to the `object`.  The path may write to multiple locations
  of the object depending on how it's defined.
* `setValue` - An object which will be written to the path or paths defined by
  `key`.

RETURNS:

Returns `true` if success, otherwise returns `false`.
*/

setObjectValue = { Object object, String key, Object setValue ->
    if(!(object instanceof Map) && !(object instanceof List)) {
        sandscapeErrorLogger("setObjectValue - object is not a Map or List.  ${object.class}")
        return
    }
    if(key.startsWith('[') && !(object instanceof List)) {
        sandscapeErrorLogger("setObjectValue - `object` is not List but `key` defines it as a List.")
        return
    }
    if(!key.startsWith('[') && !(object instanceof Map)) {
        sandscapeErrorLogger("setObjectValue - `object` is not Map but `key` defines it as a Map.")
        return
    }
    try {
        String key1, key2, nextKey
        key1 = key2 = nextKey = null
        def index, nextObject
        index = nextObject = null

        if(key.indexOf('.') >= 0) {
            key1 = key.split('\\.', 2)[0]
            key2 = key.split('\\.', 2)[1]
            //nextKey is used later to detect of a List (empty) or a Map.
            nextKey = (key2.split('\\.', 2)[0] - ~/\[(-?[0-9]*\*?)\]$/)
        }
        else {
            key1 = key
            key2 = (key - ~/\[(-?[0-9]*\*?)\]$/)
        }

        if(key1.matches(/.*\[-?[0-9]*\*?\]$/)) {
            (key1 =~ /(.*)\[(-?[0-9]*\*?)\]$/)[0].with {
                key1 = it[1]
                index = it[2]
            }
            if(index == '*') {
                //if key1 is empty then treat it as a List else a Map
                nextObject = (key1.isEmpty())? object : object[key1]
                if(nextObject.size() == 0) {
                    nextObject[0] = null
                }
                for(int i = 0; i < nextObject.size(); i++) {
                    if(key.indexOf('.') >= 0) {
                        if(nextKey.isEmpty() && !(nextObject[i] instanceof List)) {
                            //TODO what of nextKey array is `*` or `int`?
                            nextObject[i] = []
                        }
                        else if(!(nextObject[i] instanceof Map)) {
                            nextObject[i] = [:]
                        }
                        setObjectValue(nextObject[i], key2, setValue)
                    }
                    else {
                        nextObject[i] = setValue
                    }
                }
            }
            else {
                index = index.toInteger()
                if(key1.isEmpty()) {
                    if(!(object instanceof List)) {
                        object = []
                    }
                }
                else {
                    if(!(object instanceof Map)) {
                        object = [:]
                    }
                }
                //if key1 is empty then treat it as a List else a Map
                nextObject = (key1.isEmpty())? object : object[key1]
                if(key.indexOf('.') >= 0) {
                    //force the next key to be a Map or List
                    if(nextKey.isEmpty() && !(nextObject[index] instanceof List)) {
                        nextObject[index] = []
                    }
                    else if(!(nextObject[index] instanceof Map)) {
                        nextObject[index] = [:]
                    }
                    setObjectValue(nextObject[index], key2, setValue)
                }
                else {
                    nextObject[index] = setValue
                }
            }
        }
        else {
            object[key] = setValue
        }
    }
    catch(Exception e) {
        sandscapeErrorLogger(ExceptionUtils.getStackTrace(e))
        return false
    }
    return true
}

/*
This function tests the health of a URL HTTP status by calling the HTTP HEAD
method of the HTTP protocol.

USAGE:

    isUrlGood 'http://example.com'

PARAMETERS:

* `url` - A `String` which is the URL of a website.

RETURNS:

A `Boolean`, `true` if the website returns an HTTP 2XX status code and `false`
otherwise.
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
    return ((int) code / 100) == 2
}

/*
Download a file to a local `fullpath`.  If the parent directories of the path
are missing then they are automatically created (similar to the Linux command
`mkdir -p`).

USAGE:

    downloadFile('http://example.com', '/tmp/foo/index.html')

PARAMETERS:

* `url` - A `String` which is a URL to a file on a website.
* `fullpath` - A `String` which is a full file path.  It is the destination of
  the downloaded file.

RETURNS:

A `Bolean`, `true` if downloading the file was a success or `false` if not.
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
