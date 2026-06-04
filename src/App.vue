<script setup>
import { ref, onMounted, watch } from 'vue';
import { Api, TelegramClient } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import LoginView from './views/LoginView.vue';

const API_ID     = 11704522;
const API_HASH   = '4d8519ffb0302f8ac42574a44839bde3';
const BOT_USERNAME = 'Crepixbot';

// boot | login | opening | app
const state = ref('boot');

let tgClient = null;

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
        try {
            tgClient = new TelegramClient(new StringSession(saved), API_ID, API_HASH, {
                connectionRetries: 5,
            });
            await tgClient.connect();
            await openMiniApp();
        } catch {
            state.value = 'login';
        }
    } else {
        state.value = 'login';
    }
});

async function onLoggedIn({ client }) {
    tgClient = client;
    await openMiniApp();
}

async function openMiniApp() {
    state.value = 'opening';

    let url = null;

    try {
        // ════════════════════════════════════════════════════════════════
        // RequestMainWebView — نفس الـ API اللي يستخدمه Telegram الرسمي
        // يرجع URL كامل بـ tgWebAppData حقيقي وموقّع من سيرفرات Telegram
        // Mini App تتعرف على المستخدم كأنه يستخدمها من Telegram الرسمي
        // ════════════════════════════════════════════════════════════════
        const botPeer = await tgClient.getInputEntity(BOT_USERNAME);
        const result  = await tgClient.invoke(
            new Api.messages.RequestMainWebView({
                peer:     botPeer,
                bot:      botPeer,
                platform: 'android',
            })
        );
        url = result.url;
    } catch {
        try {
            const botPeer = await tgClient.getInputEntity(BOT_USERNAME);
            const result  = await tgClient.invoke(
                new Api.messages.RequestSimpleWebView({
                    bot:      botPeer,
                    url:      'https://shoof-tv.net',
                    platform: 'android',
                })
            );
            url = result.url;
        } catch {
            url = 'https://shoof-tv.net';
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // بدلاً من iframe → نتنقل بـ WebView الكامل إلى المينيآب
    // هذا يسمح لـ TelegramWebviewProxy المحقون في MainActivity
    // بالعمل مباشرة في نفس context الصفحة (لا قيود CORS)
    // ════════════════════════════════════════════════════════════════════
    state.value = 'app';
    window.location.href = url;
}
</script>

<template>
<div class="fixed inset-0 bg-[#080808]" style="font-family:'Cairo',sans-serif">

    <!-- شاشة الإقلاع -->
    <div v-if="state === 'boot'"
        class="flex items-center justify-center h-full">
        <div class="w-20 h-20 rounded-[1.5rem]"
            style="background:linear-gradient(135deg,#e5001a,#7a0011)">
        </div>
    </div>

    <!-- شاشة الدخول -->
    <LoginView v-else-if="state === 'login'" @logged-in="onLoggedIn" />

    <!-- شاشة الانتقال — تظهر ثم تختفي بمجرد فتح Mini App -->
    <div v-else-if="state === 'opening' || state === 'app'"
        class="flex flex-col items-center justify-center h-full gap-8 px-8">
        <div class="relative flex items-center justify-center">
            <div class="absolute w-36 h-36 rounded-full border-2 opacity-10 animate-ping"
                style="border-color:#e5001a"></div>
            <div class="absolute w-28 h-28 rounded-full border-2 opacity-20 animate-ping"
                style="border-color:#e5001a; animation-delay:0.25s"></div>
            <div class="absolute w-20 h-20 rounded-full border-2 opacity-30 animate-ping"
                style="border-color:#e5001a; animation-delay:0.5s"></div>
            <div class="w-16 h-16 rounded-[1.25rem] flex items-center justify-center"
                style="background:linear-gradient(135deg,#e5001a,#7a0011);box-shadow:0 16px 48px rgba(229,0,26,0.5)">
                <svg width="30" height="30" viewBox="0 0 56 56" fill="none">
                    <path d="M14 14 L14 42 L44 28 Z" fill="white"/>
                </svg>
            </div>
        </div>
        <div class="text-center">
            <p class="font-bold text-lg text-white">شوف TV</p>
            <p class="text-sm mt-1" style="color:#444">جاري فتح التطبيق...</p>
        </div>
    </div>

</div>
</template>
