<script setup>
import { ref, onMounted } from 'vue';
import LoginView from './views/LoginView.vue';

const isLoggedIn = ref(false);
const tgUserId = ref('');

onMounted(async () => {
    let saved = null;
    try {
        const { Preferences } = await import('@capacitor/preferences');
        const { value } = await Preferences.get({ key: 'tg_session' });
        saved = value;
    } catch {
        saved = localStorage.getItem('tg_session');
    }
    if (saved) {
        isLoggedIn.value = true;
    }
});

function onLoggedIn({ session, userId }) {
    tgUserId.value = userId || '';
    isLoggedIn.value = true;
}

function getShoofUrl() {
    const base = 'https://shoof-tv.net';
    if (tgUserId.value) return `${base}?tg_id=${encodeURIComponent(tgUserId.value)}&source=apk`;
    return base;
}
</script>

<template>
    <LoginView v-if="!isLoggedIn" @logged-in="onLoggedIn" />

    <div v-else class="fixed inset-0 bg-black">
        <iframe
            :src="getShoofUrl()"
            class="w-full h-full border-0"
            allow="autoplay; fullscreen; encrypted-media"
            allowfullscreen
        ></iframe>
    </div>
</template>
