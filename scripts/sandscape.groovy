import groovy.json.JsonSlurper
import jenkins.model.Jenkins

Jenkins jenkins = Jenkins.instance

//make available functions for Sandscape and Sandscape plugins via Groovy bindings
evaluate(new File(jenkins.root.toString() + "/sandscape/functions.groovy"))

def isShutdownModeEnabled() {
    if(jenkins.isQuietingDown()) {
        println 'Shutdown mode enabled.  Sandscape will not configure Jenkins.'
        sandscapeLogger 'Shutdown mode enabled.  Sandscape will not configure Jenkins.'
    }
    jenkins.isQuietingDown()
}

//abort script because shutdown mode is enabled
if(isShutdownModeEnabled) {
    return
}

//load the user config.json and secrets.json
File sandscape_config_file = new File(jenkins.root.toString() + "/sandscape/config.json")
//abort script because there's no config file
if(!sandscape_config_file.exists()) {
    println 'Sandscape config.json does not exist.  Skipping sandscape configuration.'
    sandscapeLogger 'Sandscape config.json does not exist.  Skipping sandscape configuration.'
    return
}
Map sandscape_config = new JsonSlurper().parseText(sandscape_config_file_path.getText('UTF-8'))
if('config_path' in sandscape_config) {
    sandscape_config_file = new File(sandscape_config['config_path'].toString())
    if(!sandscape_config_file.exists()) {
        sandscapeErrorLogger 'ERROR: sandscape config_path file defined but does not exist.  Skipping sandscape configuration.'
        return
    }
    sandscape_config = new JsonSlurper().parseText(sandscape_config_file_path.getText('UTF-8'))
}
//load the secrets
if('secrets' in sandscape_config) {
    File secrets_file = new File(sandscape_config['secrets'])
    if(!secrets_file.exists()) {
        sandscapeErrorLogger 'ERROR: sandscape secrets file defined but does not exist.  Skipping sandscape configuration.'
        return
    }
}

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
