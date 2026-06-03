<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { auth, setToken } from '../api.js';

const emit = defineEmits(['logged-in']);

const step = ref('init'); // init | waiting | error
const deepLink = ref('');
const authToken = ref('');
const errorMsg = ref('');
let pollInterval = null;

async function startLogin() {
    step.value = 'waiting';
    errorMsg.value = '';
    try {
        const data = await fetch('https://shoof-tv.net/api/v1/mobile/auth/request', {
            headers: { Accept: 'application/json' }
        }).then(r => r.json());

        authToken.value = data.token;
        deepLink.value = data.deep_link;

        // افتح تيليجرام تلقائياً
        window.open(deepLink.value, '_blank');

        // ابدأ الاستطلاع
        pollInterval = setInterval(poll, 2000);

        // انتهاء صلاحية بعد 5 دقائق
        setTimeout(() => {
            if (step.value === 'waiting') {
                clearInterval(pollInterval);
                step.value = 'error';
                errorMsg.value = 'انتهت صلاحية الرابط. حاول مرة أخرى.';
            }
        }, 300000);
    } catch {
        step.value = 'error';
        errorMsg.value = 'فشل الاتصال بالسيرفر. تحقق من الإنترنت.';
    }
}

async function poll() {
    if (!authToken.value) return;
    try {
        const data = await fetch(`https://shoof-tv.net/api/v1/mobile/auth/verify/${authToken.value}`, {
            headers: { Accept: 'application/json' }
        }).then(r => r.json());

        if (data.status === 'ok') {
            clearInterval(pollInterval);
            setToken(data.session_token);
            // حفظ الجلسة
            try {
                const { Preferences } = await import('@capacitor/preferences');
                await Preferences.set({ key: 'session_token', value: data.session_token });
                await Preferences.set({ key: 'user', value: JSON.stringify(data.user) });
            } catch {
                localStorage.setItem('session_token', data.session_token);
                localStorage.setItem('user', JSON.stringify(data.user));
            }
            emit('logged-in', data.user);
        } else if (data.status === 'expired') {
            clearInterval(pollInterval);
            step.value = 'error';
            errorMsg.value = 'انتهت صلاحية الرابط. حاول مرة أخرى.';
        }
    } catch {}
}

onUnmounted(() => clearInterval(pollInterval));
</script>

<template>
    <div class="min-h-screen flex flex-col items-center justify-center p-6 bg-gradient-to-b from-gray-900 to-black">
        <!-- شعار -->
        <div class="mb-10 text-center">
            <div class="text-6xl mb-3">🎬</div>
            <h1 class="text-3xl font-bold text-white">شوف TV</h1>
            <p class="text-gray-400 mt-2 text-sm">أفضل المسلسلات والأنيميات</p>
        </div>

        <!-- حالة الانتظار -->
        <div v-if="step === 'waiting'" class="w-full max-w-sm text-center space-y-5">
            <div class="bg-gray-800 rounded-2xl p-6">
                <div class="w-14 h-14 border-4 border-purple-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                <p class="text-white font-semibold mb-1">في انتظار تأكيد تيليجرام</p>
                <p class="text-gray-400 text-sm">افتح تيليجرام واضغط <b>Start</b> في البوت للدخول</p>
            </div>

            <button @click="window.open(deepLink, '_blank')"
                class="w-full bg-blue-600 hover:bg-blue-500 text-white font-semibold py-3.5 rounded-2xl flex items-center justify-center gap-2">
                <span class="text-xl">✈️</span>
                فتح تيليجرام مرة أخرى
            </button>

            <button @click="step = 'init'; clearInterval(pollInterval)"
                class="w-full text-gray-500 text-sm py-2">
                إلغاء
            </button>
        </div>

        <!-- حالة البداية -->
        <div v-else-if="step === 'init'" class="w-full max-w-sm space-y-4">
            <div class="bg-gray-800/50 border border-gray-700 rounded-2xl p-4 text-sm text-gray-300 text-center">
                سجّل دخولك بحساب تيليجرام لمشاهدة كل المسلسلات
            </div>
            <button @click="startLogin"
                class="w-full bg-gradient-to-r from-purple-600 to-blue-600 hover:opacity-90 text-white font-bold py-4 rounded-2xl flex items-center justify-center gap-3 text-lg shadow-lg shadow-purple-900/40">
                <span class="text-2xl">✈️</span>
                دخول بحساب تيليجرام
            </button>
        </div>

        <!-- خطأ -->
        <div v-else-if="step === 'error'" class="w-full max-w-sm space-y-4 text-center">
            <div class="bg-red-900/30 border border-red-700 rounded-2xl p-4 text-red-300 text-sm">
                {{ errorMsg }}
            </div>
            <button @click="step = 'init'"
                class="w-full bg-purple-600 hover:bg-purple-500 text-white font-bold py-3.5 rounded-2xl">
                حاول مرة أخرى
            </button>
        </div>

        <p class="mt-8 text-gray-600 text-xs text-center">
            شوف TV · جميع الحقوق محفوظة
        </p>
    </div>
</template>
