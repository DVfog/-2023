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
public class TelegramBot extends TelegramLongPollingBot {//расширение класс позволяющее общаться боту с телеграммом

    @Autowired
    private UserRepository userRepository;
    private AmmoRepository ammoRepository;
    private QuestsRepository questsRepository;
    final BotConfig config;

    static final String ERROR_TEXT = "ERROR: ";
    static final String url = "jdbc:mysql://localhost:3306/tg-bot";
    static final String username = "root";
    static final String password = "Parol1/5";
    static final String HELP_TEXT = "Этот бот создан для КУРСОВОЙ РАБОТЫ.\n\n" +
            "Вы можете выполнять команды из главного меню слева или набрав команду:\n" +
            "Введите /start чтобы начать работу.\n\n" +
            "Бот выполняет запросы пользователя по выводу информации, создавая по своей сути справочник по различным аспектам игры.";

    //В этом месте определяется конструктор класса `TelegramBot`,
    //который принимает три параметра: `ammoRepository`, `questsRepository` и `config`.
    public TelegramBot(AmmoRepository ammoRepository, QuestsRepository questsRepository, BotConfig config) {
        this.ammoRepository = ammoRepository;
        this.questsRepository = questsRepository;
        this.config = config;

        //Меню бота в нижнем левом углу
        List<BotCommand> listofCommand = new ArrayList<>();
        listofCommand.add(new BotCommand("/start", "Приветсвие бота"));
        listofCommand.add(new BotCommand("/help", "Информация по использованию бота"));
        try {//В этом месте происходит запись и обновление данных в бд из json файлов с помощью мапинга
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
            log.error(ERROR_TEXT + e.getMessage()); //создает сообщение об ошибке в лог файле
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
        //Проверка ботом чата на наличие новых сообщений
        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatID = update.getMessage().getChatId();
            //Реализация рассылки сообщений всем пользователям, перед отправкой проходит проверка чата на администраторский (тот который указан в файле конфигурации)
            if(messageText.contains("/send") && config.getOwnerID() == chatID) {
                var textToSend = EmojiParser.parseToUnicode
                        (messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users){
                    sendMessage(user.getChatID(), textToSend);
                }
            }
            else {
                //Команды для бота
                switch (messageText) {
                    //При вызове пользователем этой команды, бот вызывает метод проверки регистрации пользователя в бд.
                    case "/start", "старт", "start", "Привет", "Hi", "привет":
                        registerUser(update.getMessage());
                        startCommandReceived(chatID, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help", "помощь", "help":
                        sendMessage(chatID, HELP_TEXT);
                        break;
                    case "/quests", "квесты", "задания", "Квесты", "Quests", "quests":
                        questschoice(chatID);
                        break;
                    case "/ammo", "патроны", "Ammo", "Патроны", "ammo":
                        ammochoice(chatID);
                        break;
                    case "76239", "762x39", "7.62x39", "7,62x39", "7,62х39", "7.62х39", "762х39":
                        ammotypecase(chatID, "Caliber762x39");
                        break;
                    case "918", "9x18", "9х18":
                        ammotypecase(chatID, "Caliber9x18PM");
                        break;
                    default: //ответ бота на не определённые комнады
                        commandNotFound(chatID);
                        break;
                }
            }

        }//проверка если вместо сообщения прислали какое либо значение(нажали кнопку)
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatID = update.getCallbackQuery().getMessage().getChatId();

            try {
                String ammoType = null;
                // обработка данных
                if (callbackData.equals("Caliber9x18PM")) {
                    ammoType = "Caliber9x18PM";
                } else if (callbackData.equals("Caliber762x51")) {
                    ammoType = "Caliber762x51";
                } else if (callbackData.equals("Caliber762x39")) {
                    ammoType = "Caliber762x39";
                } else if (callbackData.equals("Caliber545x39")) {
                    ammoType = "Caliber545x39";
                } else if (callbackData.equals("Caliber556x45NATO")) {
                    ammoType = "Caliber556x45NATO";
                }

                if (ammoType != null) {
                    ammos(chatID, ammoType);
                } else {

                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void ammotypecase (long chatID, String ammo) { //Принимаем значения от нажатой кнопки и отправляем их в метод для составления запроса в бд
        try {
            ammos(chatID, ammo);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private void questschoice(long chatID) {
        SendMessage message = new SendMessage();
        sendMessage (chatID, "Функция находится в разработке, следите за новостями");
        message.setChatId(chatID);
    }

    // Метот создания экранных кнопок под сообщениями бота, которые при нажатии отправляют вместо сообщений данные
    private void ammochoice (Long chatID) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatID));
        message.setText("Выбери калибр:");
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup(); //создание экранных кнопок под сообщением
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var pmButton = new InlineKeyboardButton();
        pmButton.setText("9x18PM"); // название кнопки
        pmButton.setCallbackData("Caliber9x18PM"); //данные которые она возвращает
        var arButton = new InlineKeyboardButton();
        arButton.setText("7,62x51");
        arButton.setCallbackData("Caliber762x51");
        var akmButton = new InlineKeyboardButton();
        akmButton.setText("7,62x39");
        akmButton.setCallbackData("Caliber762x39");
        var akButton = new InlineKeyboardButton();
        akButton.setText("5,45x39");
        akButton.setCallbackData("Caliber545x39");
        var mButton = new InlineKeyboardButton();
        mButton.setText("5,56x45NATO");
        mButton.setCallbackData("Caliber556x45NATO");
        rowInLine.add(pmButton);
        rowInLine.add(arButton);
        rowInLine.add(akmButton);
        rowInLine.add(akButton);
        rowInLine.add(mButton);
        rowsInLine.add(rowInLine);
        markupInline.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }
    // Метод при вызове которого бот обращается в базу данных и выполняет указанный запрос, после чего выводит его пользователю
    // Метод составляет необходимый запрос, путем нажатия пользователем кнопки которая возвращает данные
    // для этого метода в которых указан необходимое значение для подстановки в запрос
    private void ammos (long chatID, String ammo) throws SQLException{
        Connection connection = DriverManager.getConnection(url, username, password);
        String query = "SELECT * FROM ammo where caliber like ";
        query = query + "'" + ammo + "'" + ";";
        System.out.println(query);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) { // Отправка информации в чат
            String name = resultSet.getString("name");
            String damage = resultSet.getString("damage");
            String penetrationPower = resultSet.getString("penetration_power");
            sendMessage (chatID,"Патрон: " + name + ", \nнаносит урон: "
                    + damage + ", с пробитием: " +penetrationPower+ ".");
            SendMessage message = new SendMessage();
            message.setChatId(chatID);
         }
    }

    //метод ответа на неопределеные сообщения
    private void commandNotFound(long chatID) {
        String answer = EmojiParser.parseToUnicode("Неизвестная команда" + " :neutral_face:");
        sendMessage (chatID, answer);
        log.info("Replied to user: " + chatID); //создает сообщение в лог файле об ответе бота пользователю
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
            log.info("registered a user: " + user, chatId);
            //отправляет запись в лог файл об регистрации пользователя

        }
    }

    //Метод приветсвия бота
    private void startCommandReceived(Long chatID, String name) {
        String answer = EmojiParser.parseToUnicode("Здравствуйте, " + name +  ". Этот бот создан для помощи людям играющим в Eft. \nCоблюдайте правила использования платформы." + ":blush:" );
        log.info("Greeted the user: " + name, chatID);
        sendMessage(chatID, answer);

    }
        //Метод для отправки сообщений
    private void sendMessage(long chatID, @NonNull String textToSend){
        SendMessage message = new SendMessage ();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        //Создание экранной клавиатуры бота которая отправляет готовые сообщения
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
        executeMessage(message);
    }
    //Этот метод используется для выполнения запроса на отправку сообщения через Telegram API.
    // Он принимает объект `SendMessage`, который представляет собой запрос на отправку сообщения.
    private void executeMessage(SendMessage message){
        try{
            execute(message);
        }
        catch (TelegramApiException e){
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
}
