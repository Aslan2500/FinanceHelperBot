package finance.bot.entity.texts;

public enum CommandTexts {
    HELP_TEXT("""
            This bot will help you to get financial reports for 5 previous years.
            You have to type company's ticker to get it's report, for example 'AAPL'.
            Data is taken from www.sec.gov"""
    );

    private final String text;

    CommandTexts(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
