<script setup>
import { ref, onMounted } from 'vue';
import { Api, TelegramClient } from 'telegram';
import { StringSession } from 'telegram/sessions/index.js';
import LoginView from './views/LoginView.vue';

const API_ID     = 11704522;
const API_HASH   = '4d8519ffb0302f8ac42574a44839bde3';
const BOT_USERNAME = 'Crepixbot';

// boot = بدء التشغيل | login = شاشة الدخول | opening = جاري الفتح | app = التطبيق
const state = ref('boot');
const miniAppUrl = ref('');

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

    try {
        // ══════════════════════════════════════════════════════════════
        // RequestMainWebView — نفس الـ API اللي يستخدمه Telegram الرسمي
        // يرجع URL كامل بـ initData حقيقي وموقّع من سيرفرات Telegram
        // المستخدم يظهر للـ Mini App كأنه فتحها من تطبيق Telegram تماماً
        // ══════════════════════════════════════════════════════════════
        const botPeer = await tgClient.getInputEntity(BOT_USERNAME);

        const result = await tgClient.invoke(
            new Api.messages.RequestMainWebView({
                peer:     botPeer,
                bot:      botPeer,
                platform: 'android',
            })
        );

        miniAppUrl.value = result.url;
        state.value = 'app';

    } catch {
        // fallback 1: RequestSimpleWebView
        try {
            const botPeer = await tgClient.getInputEntity(BOT_USERNAME);
            const result = await tgClient.invoke(
                new Api.messages.RequestSimpleWebView({
                    bot:      botPeer,
                    url:      'https://shoof-tv.net',
                    platform: 'android',
                })
            );
            miniAppUrl.value = result.url;
            state.value = 'app';

        } catch {
            // fallback 2: URL مباشر
            miniAppUrl.value = 'https://shoof-tv.net';
            state.value = 'app';
        }
    }
}
</script>

<template>
<div class="fixed inset-0 bg-[#080808]" style="font-family:'Cairo',sans-serif">

    <!-- شاشة الإقلاع -->
    <div v-if="state === 'boot'"
        class="flex items-center justify-center h-full">
        <div class="w-20 h-20 rounded-[1.5rem]"
            style="background:linear-gradient(135deg,#e5001a,#7a0011);box-shadow:0 16px 48px rgba(229,0,26,0.4)">
        </div>
    </div>

    <!-- شاشة الدخول -->
    <LoginView
        v-else-if="state === 'login'"
        @logged-in="onLoggedIn"
    />

    <!-- شاشة فتح التطبيق - جاري تحميل Mini App -->
    <div v-else-if="state === 'opening'"
        class="flex flex-col items-center justify-center h-full gap-8 px-8">
        <div class="relative flex items-center justify-center">
            <div class="absolute w-32 h-32 rounded-full border-2 opacity-20 animate-ping"
                style="border-color:#e5001a"></div>
            <div class="absolute w-24 h-24 rounded-full border-2 opacity-30 animate-ping"
                style="border-color:#e5001a; animation-delay:0.3s"></div>
            <div class="w-20 h-20 rounded-[1.5rem] flex items-center justify-center"
                style="background:linear-gradient(135deg,#e5001a,#7a0011);box-shadow:0 16px 48px rgba(229,0,26,0.4)">
                <svg width="36" height="36" viewBox="0 0 56 56" fill="none">
                    <path d="M14 14 L14 42 L44 28 Z" fill="white"/>
                </svg>
            </div>
        </div>
        <div class="text-center">
            <p class="font-bold text-lg text-white">شوف TV</p>
            <p class="text-sm mt-2" style="color:#555">جاري فتح التطبيق...</p>
        </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         Mini App — مفتوحة بـ initData حقيقي من Telegram
         المستخدم محاصر بداخلها، لا يوجد شريط عنوان URL
         ══════════════════════════════════════════════════════ -->
    <div v-else-if="state === 'app'" class="fixed inset-0 bg-black">
        <iframe
            :src="miniAppUrl"
            class="w-full h-full border-0"
            allow="autoplay; fullscreen; encrypted-media; camera; microphone"
            allowfullscreen
            referrerpolicy="no-referrer-when-downgrade"
        ></iframe>
    </div>

</div>
</template>
