package dev.struchkov.example.quarkus.bot.config;

import dev.struchkov.example.quarkus.bot.unit.MainUnitConfig;
import dev.struchkov.godfather.main.core.unit.TypeUnit;
import dev.struchkov.godfather.main.domain.content.Mail;
import dev.struchkov.godfather.quarkus.context.service.UnitPointerService;
import dev.struchkov.godfather.quarkus.core.action.AnswerSaveAction;
import dev.struchkov.godfather.quarkus.core.action.cmd.RollBackCmdAction;
import dev.struchkov.godfather.quarkus.core.provider.StoryLineHandler;
import dev.struchkov.godfather.quarkus.core.service.PersonSettingServiceImpl;
import dev.struchkov.godfather.quarkus.core.service.StorylineContextMapImpl;
import dev.struchkov.godfather.quarkus.core.service.StorylineMailService;
import dev.struchkov.godfather.quarkus.core.service.StorylineService;
import dev.struchkov.godfather.quarkus.core.service.UnitPointerServiceImpl;
import dev.struchkov.godfather.quarkus.data.StorylineContext;
import dev.struchkov.godfather.quarkus.data.repository.impl.PersonSettingLocalRepository;
import dev.struchkov.godfather.quarkus.data.repository.impl.StorylineMapRepository;
import dev.struchkov.godfather.quarkus.data.repository.impl.UnitPointLocalRepository;
import dev.struchkov.godfather.telegram.domain.config.ProxyConfig;
import dev.struchkov.godfather.telegram.domain.config.TelegramConnectConfig;
import dev.struchkov.godfather.telegram.main.context.TelegramConnect;
import dev.struchkov.godfather.telegram.quarkus.consumer.EventDistributorService;
import dev.struchkov.godfather.telegram.quarkus.context.service.EventDistributor;
import dev.struchkov.godfather.telegram.quarkus.context.service.TelegramSending;
import dev.struchkov.godfather.telegram.quarkus.core.MailAutoresponderTelegram;
import dev.struchkov.godfather.telegram.quarkus.core.TelegramConnectBot;
import dev.struchkov.godfather.telegram.quarkus.sender.TelegramSender;
import io.quarkus.runtime.Startup;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import java.util.List;

@ApplicationScoped
public class TelegramConfig {

    @Singleton
    public ProxyConfig proxyConfig(
            @ConfigProperty(name = "telegram-bot.telegram-config.proxy.host") String host,
            @ConfigProperty(name = "telegram-bot.telegram-config.proxy.port") Integer port,
            @ConfigProperty(name = "telegram-bot.telegram-config.proxy.password") String password,
            @ConfigProperty(name = "telegram-bot.telegram-config.proxy.username") String username
    ) {
        final ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setHost(host);
        proxyConfig.setPort(port);
        proxyConfig.setUser(username);
        proxyConfig.setPassword(password);
        proxyConfig.setType(ProxyConfig.Type.SOCKS5);
        return proxyConfig;
    }

    /**
     * Учетные данные для общения с Telegram
     *
     * @param botUsername имя бота, не включая @
     * @param botToken    токен доступа к боту
     */
    @ApplicationScoped
    public TelegramConnectConfig telegramConfig(
            @ConfigProperty(name = "telegram-bot.telegram-config.bot-username") String botUsername,
            @ConfigProperty(name = "telegram-bot.telegram-config.bot-token") String botToken,
            @ConfigProperty(name = "telegram-bot.telegram-config.proxy.enable") Boolean enableProxy,
            ProxyConfig proxyConfig
    ) {
        final TelegramConnectConfig telegramConnectConfig = new TelegramConnectConfig(botUsername, botToken);
        if (Boolean.TRUE.equals(enableProxy)) {
            telegramConnectConfig.setProxyConfig(proxyConfig);
        }
        return telegramConnectConfig;
    }

    /**
     * Объект используется для выполнения запросов к телеграму.
     *
     * @param telegramConfig Объект с данными для подключения к телеграм
     */
    @ApplicationScoped
    public TelegramConnectBot telegramConnect(
            TelegramConnectConfig telegramConfig
    ) {
        return new TelegramConnectBot(telegramConfig);
    }

    /**
     * Сервис отвечает за сохранение позиции пользователя в сценарии.
     */
    @ApplicationScoped
    public UnitPointerService unitPointerService() {
        return new UnitPointerServiceImpl(new UnitPointLocalRepository());
    }

    /**
     * Сервис для работы со сценарием
     *
     * @param unitPointerService Сервис работающий с текущей позицией пользователя в сценарии
     */
    @ApplicationScoped
    public StorylineService<Mail> mailStorylineService(
            UnitPointerService unitPointerService,

            MainUnitConfig mainUnitConfig
    ) {
        final List<Object> unitConfigurations = List.of(
                mainUnitConfig
        );

        return new StorylineMailService(
                unitPointerService,
                new StorylineMapRepository(),
                unitConfigurations
        );
    }

    @ApplicationScoped
    public StorylineContext storylineContext() {
        return new StorylineContextMapImpl();
    }

    /**
     * Сервис отвечает за всю обработку сценария.
     *
     * @param sending          Сервис для отправки сообщений в телеграм
     * @param storylineService Сервис для работы со сценарием
     */
    @Singleton
    public MailAutoresponderTelegram mailAutoresponderTelegram(
            TelegramSending sending,
            StorylineService<Mail> storylineService
    ) {
        final MailAutoresponderTelegram messageAutoresponderTelegram = new MailAutoresponderTelegram(
                sending,
                new PersonSettingServiceImpl(new PersonSettingLocalRepository()),
                storylineService
        );
        messageAutoresponderTelegram.initActionUnit(TypeUnit.BACK_CMD, new RollBackCmdAction<>(storylineService));
        messageAutoresponderTelegram.initSaveAction(new AnswerSaveAction<>());
        return messageAutoresponderTelegram;
    }

    @ApplicationScoped
    public StoryLineHandler eventStoryLineProvider(
            MailAutoresponderTelegram messageAutoresponderTelegram
    ) {
        return new StoryLineHandler(messageAutoresponderTelegram);
    }

    @Startup
    @Singleton
    public EventDistributor eventDistributor(
            TelegramConnectBot telegramConnect,
            StoryLineHandler storyLineHandler
    ) {
        return new EventDistributorService(
                telegramConnect,
                List.of(storyLineHandler)
        );
    }

    @Singleton
    public TelegramSending telegramSending(
            TelegramConnect telegramConnect
    ) {
        return new TelegramSender(telegramConnect);
    }

}
