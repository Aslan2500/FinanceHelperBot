package finance.bot.service;

import com.vdurmont.emoji.EmojiParser;
import finance.bot.config.BotConfig;
import finance.bot.entity.User;
import finance.bot.entity.texts.CommandTexts;
import finance.bot.repository.UserRepository;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Press to start using this bot" ));
        listOfCommands.add(new BotCommand("/help", "How to use this bot" ));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/help":
                    sendMessage(chatId, CommandTexts.HELP_TEXT.toString());
                default:
                    stockReport(chatId, update.getMessage());
            }
        }
    }

    private void registerUser(Message message) {
        if (!userRepository.findById(message.getChatId()).isPresent()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();
            User user = new User();
            user.setId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
        }
    }

    private void startCommandReceived(long chatId, String firstName) {
        String answer = EmojiParser.parseToUnicode("Hi, " + firstName + ", nice to meet you! :blush:" );
        sendMessage(chatId, answer);
    }

    private void stockReport(long chatId, Message message) {
        try {
            URL url = new URL("https://financialmodelingprep.com/api/v3/income-statement/" + message.getText() + "?apikey=83cf4ee49eed09add7c13f7049ccb5ad" );
            String json = IOUtils.toString(url, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                sendMessage(
                        chatId, "Link - " + jsonArray.getJSONObject(i).get("finalLink" ).toString() + "\n" +
                                "Revenue: " + jsonArray.getJSONObject(i).get("revenue" ) + "\n" +
                                "Eps: " + jsonArray.getJSONObject(i).get("eps" ) + "\n" +
                                "Ebitda: " + jsonArray.getJSONObject(i).get("ebitda" ) + "\n" +
                                "Net Income: " + jsonArray.getJSONObject(i).get("netIncome" )
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
