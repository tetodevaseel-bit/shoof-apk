import './style.css';
import { createApp, ref } from 'vue';
import router from './router/index.js';
import { setToken } from './api.js';
import LoginView from './views/LoginView.vue';
import App from './App.vue';

async function bootstrap() {
    // محاولة استعادة الجلسة المحفوظة
    let savedToken = null;
    try {
        const { Preferences } = await import('@capacitor/preferences');
        const { value } = await Preferences.get({ key: 'session_token' });
        savedToken = value;
    } catch {
        savedToken = localStorage.getItem('session_token');
    }

    if (savedToken) {
        setToken(savedToken);
    }

    createApp(App, { initialToken: savedToken }).use(router).mount('#app');
}

bootstrap();
