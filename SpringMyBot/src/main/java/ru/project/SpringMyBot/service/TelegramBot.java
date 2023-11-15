package ru.project.SpringMyBot.service;


import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project.SpringMyBot.config.BotConfig;
import ru.project.SpringMyBot.model.User;
import ru.project.SpringMyBot.model.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "This bot is created to КУРСОВАЯ РАБОТА.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n";


    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommand= new ArrayList<>();
        listofCommand.add (new BotCommand("/start","get a welcome message"));
        listofCommand.add (new BotCommand("/mydata", "get your data stored"));
        listofCommand.add (new BotCommand("/help","info to used this bot "));
        listofCommand.add (new BotCommand("/settings", "set your preference "));
        try{
            this.execute(new SetMyCommands(listofCommand,new BotCommandScopeDefault(),null));
        }
        catch (TelegramApiException e){
        }

        }



    @Override
    public String getBotUsername() { return config.getBotName(); }

    @Override
    public String getBotToken() { return config.getToken(); }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
            String messegeText = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();
            switch (messegeText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatID, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatID, HELP_TEXT);
                    break;
                case "Люблю Улика":
                    sendMessage(chatID,"Улик тоже тебя любит");
                    break;
                default:
                    sendMessage(chatID, "Sorry");

            }
        }
    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()){
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();
            user.setChatID(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);

        }
    }

    private void startCommandReceived(Long chatID, String name) {
        String answer = "hi," + name;
        sendMessage(chatID, answer);

    }

    private void sendMessage(long chatID, @NonNull String textToSend){
        SendMessage message = new SendMessage ();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("get random joke");

        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("Люблю Улика");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        try{
            execute(message);
        }
        catch (TelegramApiException e){
        }
    }
}
