    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, NotificationHelper.buildServiceNotification(this))
        registerActivityTransitions()
        TtsHelper.init(this) // מחמם את מנוע הקול מראש כדי שיהיה מוכן מיד כשמתחילים לנסוע
    }
