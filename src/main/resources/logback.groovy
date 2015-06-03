appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%date [%-5level] [%thread] %message%n'
    }
}

root INFO, ['CONSOLE']
