package ru.project.SpringMyBot.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project.SpringMyBot.config.BotConfig;
import ru.project.SpringMyBot.model.*;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component //позволяет автоматически создать экземпляр Спрингу
public class TelegramBot extends TelegramLongPollingBot {//расширение класс позволяющее общаться юоту с телеграммом

    @Autowired
    private UserRepository userRepository;

    private AmmoRepository ammoRepository;
    private QuestsRepository questsRepository;
    final BotConfig config;


    static final String url = "jdbc:mysql://localhost:3306/tg-bot";
    static final String username = "root";
    static final String password = "Parol1/5";
    static final String HELP_TEXT = "Этот бот создан для КУРСОВОЙ РАБОТЫ.\n\n" +
            "Вы можете выполнять команды из главного меню слева или набрав команду:\n\n" +
            "Введите /start чтобы начать работу.\n";

    //Конструктор
    public TelegramBot(AmmoRepository ammoRepository, QuestsRepository questsRepository, BotConfig config) {
        this.ammoRepository = ammoRepository;
        this.questsRepository = questsRepository;
        this.config = config;

        //Меню бота в нижнем левом углу
        List<BotCommand> listofCommand = new ArrayList<>();
        listofCommand.add(new BotCommand("/start", "Приветсвие бота"));
        listofCommand.add(new BotCommand("/mydata", "Показать мою информацию"));
        listofCommand.add(new BotCommand("/help", "Информация по использованию бота"));
        listofCommand.add(new BotCommand("/settings", "Настройки"));
        listofCommand.add(new BotCommand("/register", "register"));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            List<quests> questsList = objectMapper.readValue(new File("db/quests.json"),
                    typeFactory.constructCollectionType(List.class, quests.class));
            List<ammo> ammoList = objectMapper.readValue(new File("db/ammo.json"),
                    typeFactory.constructCollectionType(List.class, ammo.class));
            this.questsRepository.saveAll(questsList);
            this.ammoRepository.saveAll(ammoList);


        }
        catch(Exception e) {
            log.error("Error: " + e.getMessage()); //создает сообщение об ошибке в лог файле
        }
    }






    @Override
    //бот передает телеграмму свое имя
    public String getBotUsername() { return config.getBotName(); }

    @Override
    //бот передает телграмму свой токен
    public String getBotToken() { return config.getToken(); }

    @Override
    //Главный, центтральный метод всего приложение,
    //тут происхдит обработка входных данных и возвращает ответ.
    public void onUpdateReceived(Update update) {
        //Проверка ботом чата на начличие новых сообщений
        if(update.hasMessage() && update.getMessage().hasText()){
            String messegeText = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();
            //Команды для бота
            switch (messegeText) {
                //При вызове пользователем этой команды, бот вызывает метод проверки регистрации пользователя в бд.
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatID, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    sendMessage(chatID, HELP_TEXT);
                    break;
                case "/Люблю Улика":
                    sendMessage(chatID,"Улик тоже тебя любит");
                    break;
                case "/register":
                    register(chatID); //
                    break;
                default: //ответ бота на не определённые комнады
                    commandNotFound(chatID);
                    break;
                case "/quests":
                    try {
                        questschoise(chatID);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/ammo":
                    try {
                        ammochoice(chatID);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }

        }//проверка если вместо сообщения прислали какое либо значение(нажали кнопку)
        //если ппользователь нажал кнопку то бот меняет содеражние сообщения.
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            //Для изменения сообщения нужно получить id собщения.
            long messageID = update.getCallbackQuery().getMessage().getMessageId();
            long chatID = update.getCallbackQuery().getMessage().getChatId();

            if(callbackData.equals("YES_BUTTON")){
                String text = "You pressed YES BUTTON";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatID));
                message.setText(text);
                message.setMessageId((int)(messageID)); //Мы указываем чтобы новое сообщение конкретно заменило текст
                //а не отправило новое сообщение
                try{
                    execute(message);
                }
                catch (TelegramApiException e){
                    log.error("Error: " + e.getMessage());
                }
            }
            else  if(callbackData.equals("NO_BUTTON")){
                String text = "You pressed NO BUTTON";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatID));
                message.setText(text);
                message.setMessageId((int)(messageID));
                try{
                    execute(message);
                }
                catch (TelegramApiException e){
                    log.error("Error: " + e.getMessage());
                }
            }
        }
    }

    private void ammochoice(long chatID) throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);

        // Выполнение запроса
        String query = "SELECT * FROM ammo WHERE id = 1";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(0,1);

        ResultSet resultSet = statement.executeQuery();

        // Отправка информации в чат
        if (resultSet.next()) {
            String name = resultSet.getString("name");
            String caliber = resultSet.getString("caliber");
            sendMessage (chatID,"Name: " + name + ", Caliber: " + caliber);
            SendMessage message = new SendMessage();
            message.setChatId(chatID);
            }
        }



    private void questschoise(long chatID) throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);
        String query = "SELECT * FROM quests";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            String title = resultSet.getString("title" );
            String dealer = resultSet.getString("dealer" );
            sendMessage(chatID,"tile:"+title+ "dealer:"+dealer);
            SendMessage message = new SendMessage();
            message.setChatId(chatID);
        }
    }

    //метод ответа на неопределеные сообщения
    private void commandNotFound(long chatID) {
        String answer = EmojiParser.parseToUnicode("Я не знаю такую команду." + " :neutral_face:");
        sendMessage (chatID, answer);
        log.info("Replied to user: " + chatID); //создает сообщение в лог файле об ответе бота пользователю
    }


    // Метот создания экранных кнопок под сообщениями бота
        private void register(long chatId) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Do you really want to register?");

            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();

            var yesButton = new InlineKeyboardButton();
            yesButton.setText("Yes");
            yesButton.setCallbackData("YES_BUTTON");

            var noButton = new InlineKeyboardButton();
            noButton.setText("No");
            noButton.setCallbackData("NO_BUTTON");

            rowInLine.add(yesButton);
            rowInLine.add(noButton);
            rowsInLine.add(rowInLine);

            markupInline.setKeyboard(rowsInLine);
            message.setReplyMarkup(markupInline);

            try{
                execute(message);
            }
            catch (TelegramApiException e){
                log.error("Error: " + e.getMessage());
            }

        }

        //Тут бот сначала сверяет пользователя в базе данных и если не находит зарегистрированного пользователя, то регистрирует его
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

            log.info("registered a user: " + user, chatId); //отправляет запись в лог файл об регистрации пользователя

        }
    }

    //Метод приветсвия бота
    private void startCommandReceived(Long chatID, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, Тарковчанин!  " + name +  ", чем я могу помочь ?" + ":blush:" );
        log.info("Greeted the user: " + name, chatID);
        sendMessage(chatID, answer);

    }
        //Метод для отправки сообщений
    private void sendMessage(long chatID, @NonNull String textToSend){
        SendMessage message = new SendMessage ();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        //Создание экранной клавиатуры бота.
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Квесты");
        row.add("Патроны");

        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Броня");
        row.add("Сборки оружия");
        row.add("Новости");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        try{
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error: " + e.getMessage());
        }
    }
}
