<script setup>
import { ref, onMounted } from 'vue';
import LoginView from './views/LoginView.vue';

const isLoggedIn = ref(false);
const sessionToken = ref('');
const webviewUrl = ref('');

onMounted(async () => {
    let saved = null;
    try {
        const { Preferences } = await import('@capacitor/preferences');
        const { value } = await Preferences.get({ key: 'session_token' });
        saved = value;
    } catch {
        saved = localStorage.getItem('session_token');
    }
    if (saved) {
        sessionToken.value = saved;
        buildUrl(saved);
        isLoggedIn.value = true;
    }
});

function buildUrl(token) {
    // نفتح شوف مع حقن التوكن كمعامل
    webviewUrl.value = `https://shoof-tv.net?mobile_token=${encodeURIComponent(token)}`;
}

function onLoggedIn(token) {
    sessionToken.value = token;
    buildUrl(token);
    isLoggedIn.value = true;
}
</script>

<template>
    <LoginView v-if="!isLoggedIn" @logged-in="onLoggedIn" />

    <!-- WebView كاملة تعرض تطبيق شوف بعد الدخول -->
    <div v-else class="fixed inset-0 bg-black">
        <iframe
            :src="webviewUrl"
            class="w-full h-full border-0"
            allow="autoplay; fullscreen; encrypted-media"
            allowfullscreen
        ></iframe>
    </div>
</template>
