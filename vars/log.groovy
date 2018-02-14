def info(message) {
    ansiColor('xterm') {
        echo "\u001B[32mINFO: ${message}\u001B[m"
    }
}

def warning(message) {
    ansiColor('xterm') {
        echo "\u001B[31mWARNING: ${message}\u001B[m"
    }
}
