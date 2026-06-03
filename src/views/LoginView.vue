<script setup>
import { ref } from 'vue';

const emit = defineEmits(['logged-in']);

const step = ref('phone'); // phone | code | loading
const phone = ref('');
const code = ref('');
const countryCode = ref('+966');
const error = ref('');
const requestToken = ref('');
const sending = ref(false);

const countries = [
    { code: '+966', name: 'السعودية', flag: '🇸🇦' },
    { code: '+971', name: 'الإمارات', flag: '🇦🇪' },
    { code: '+965', name: 'الكويت', flag: '🇰🇼' },
    { code: '+974', name: 'قطر', flag: '🇶🇦' },
    { code: '+973', name: 'البحرين', flag: '🇧🇭' },
    { code: '+968', name: 'عُمان', flag: '🇴🇲' },
    { code: '+962', name: 'الأردن', flag: '🇯🇴' },
    { code: '+961', name: 'لبنان', flag: '🇱🇧' },
    { code: '+20',  name: 'مصر', flag: '🇪🇬' },
    { code: '+964', name: 'العراق', flag: '🇮🇶' },
    { code: '+963', name: 'سوريا', flag: '🇸🇾' },
    { code: '+967', name: 'اليمن', flag: '🇾🇪' },
    { code: '+1',   name: 'أمريكا', flag: '🇺🇸' },
    { code: '+44',  name: 'بريطانيا', flag: '🇬🇧' },
    { code: '+90',  name: 'تركيا', flag: '🇹🇷' },
    { code: '+7',   name: 'روسيا', flag: '🇷🇺' },
];

const showCountry = ref(false);
const selectedCountry = ref(countries[0]);

function selectCountry(c) {
    selectedCountry.value = c;
    countryCode.value = c.code;
    showCountry.value = false;
}

async function sendCode() {
    if (!phone.value.trim() || sending.value) return;
    sending.value = true;
    error.value = '';
    try {
        const res = await fetch('https://shoof-tv.net/api/v1/mobile/auth/request', {
            headers: { 'Accept': 'application/json' }
        });
        const data = await res.json();
        requestToken.value = data.token;

        // فتح البوت بتيليجرام
        window.open(data.deep_link, '_blank');
        step.value = 'code';
    } catch {
        error.value = 'تعذّر الاتصال. تحقق من الإنترنت.';
    } finally { sending.value = false; }
}

let pollTimer = null;
function startPolling() {
    pollTimer = setInterval(async () => {
        try {
            const res = await fetch(`https://shoof-tv.net/api/v1/mobile/auth/verify/${requestToken.value}`, {
                headers: { 'Accept': 'application/json' }
            });
            const data = await res.json();
            if (data.status === 'ok') {
                clearInterval(pollTimer);
                try {
                    const { Preferences } = await import('@capacitor/preferences');
                    await Preferences.set({ key: 'session_token', value: data.session_token });
                } catch {
                    localStorage.setItem('session_token', data.session_token);
                }
                step.value = 'loading';
                setTimeout(() => emit('logged-in', data.session_token), 800);
            } else if (data.status === 'expired') {
                clearInterval(pollTimer);
                error.value = 'انتهت الصلاحية. حاول مجدداً.';
                step.value = 'phone';
            }
        } catch {}
    }, 2000);
}

function onCodeStep() {
    startPolling();
}
</script>

<template>
    <div dir="rtl" class="min-h-screen bg-[#17212b] text-white flex flex-col">

        <!-- Loading -->
        <div v-if="step === 'loading'"
            class="flex-1 flex flex-col items-center justify-center gap-4">
            <div class="w-16 h-16 rounded-full bg-[#2b5278] flex items-center justify-center text-4xl animate-pulse">🎬</div>
            <p class="text-[#aab8c2]">جاري الدخول...</p>
        </div>

        <!-- Phone step -->
        <template v-else-if="step === 'phone'">
            <div class="flex-1 flex flex-col items-center justify-center px-6 gap-6">
                <!-- Logo -->
                <div class="w-24 h-24 rounded-full bg-gradient-to-br from-[#2b5278] to-[#1c3a5a] flex items-center justify-center text-5xl shadow-2xl">
                    🎬
                </div>
                <div class="text-center">
                    <h1 class="text-2xl font-bold">شوف TV</h1>
                    <p class="text-[#aab8c2] text-sm mt-1">أدخل رقم هاتفك للمتابعة</p>
                </div>

                <!-- Country picker -->
                <div class="w-full">
                    <button @click="showCountry = !showCountry"
                        class="w-full bg-[#232e3c] rounded-xl px-4 py-3.5 flex items-center justify-between border border-[#2b3a4a]">
                        <span class="flex items-center gap-2 text-sm">
                            <span class="text-xl">{{ selectedCountry.flag }}</span>
                            <span>{{ selectedCountry.name }}</span>
                        </span>
                        <span class="text-[#aab8c2] flex items-center gap-2">
                            <span>{{ selectedCountry.code }}</span>
                            <span class="text-xs">▼</span>
                        </span>
                    </button>

                    <!-- Country list -->
                    <div v-if="showCountry"
                        class="mt-1 bg-[#232e3c] border border-[#2b3a4a] rounded-xl overflow-hidden max-h-52 overflow-y-auto">
                        <button v-for="c in countries" :key="c.code"
                            @click="selectCountry(c)"
                            class="w-full px-4 py-3 flex items-center gap-3 text-sm hover:bg-[#2b3a4a] text-right">
                            <span class="text-xl">{{ c.flag }}</span>
                            <span class="flex-1">{{ c.name }}</span>
                            <span class="text-[#aab8c2]">{{ c.code }}</span>
                        </button>
                    </div>
                </div>

                <!-- Phone input -->
                <div class="w-full bg-[#232e3c] rounded-xl border border-[#2b3a4a] flex items-center overflow-hidden">
                    <span class="px-3 text-[#aab8c2] text-sm border-l border-[#2b3a4a] py-3.5">{{ selectedCountry.code }}</span>
                    <input v-model="phone" type="tel" placeholder="رقم الهاتف"
                        class="flex-1 bg-transparent px-3 py-3.5 text-sm outline-none placeholder-[#4a6278]"
                        @keyup.enter="sendCode" />
                </div>

                <p v-if="error" class="text-red-400 text-xs text-center">{{ error }}</p>

                <button @click="sendCode" :disabled="!phone || sending"
                    class="w-full bg-[#2b5278] hover:bg-[#3a6a9a] disabled:opacity-40 py-3.5 rounded-xl font-semibold transition">
                    {{ sending ? 'جاري الإرسال...' : 'التالي' }}
                </button>
            </div>

            <p class="text-center text-[#aab8c2] text-xs pb-8">
                سيُرسَل رمز التحقق عبر تيليجرام
            </p>
        </template>

        <!-- Code step -->
        <template v-else-if="step === 'code'" @vue:mounted="onCodeStep">
            <div class="flex-1 flex flex-col items-center justify-center px-6 gap-6">
                <div class="w-20 h-20 rounded-full bg-[#2b5278] flex items-center justify-center text-4xl">✈️</div>
                <div class="text-center">
                    <h2 class="text-xl font-bold">رمز التحقق</h2>
                    <p class="text-[#aab8c2] text-sm mt-2">
                        افتح تيليجرام واضغط <b class="text-white">Start</b> في بوت شوف للدخول
                    </p>
                </div>

                <!-- OTP display -->
                <div class="bg-[#232e3c] border border-[#2b5278] rounded-2xl p-5 w-full text-center">
                    <div class="flex items-center justify-center gap-2 mb-2">
                        <div class="w-3 h-3 rounded-full bg-[#2b5278] animate-bounce" style="animation-delay:0s"></div>
                        <div class="w-3 h-3 rounded-full bg-[#2b5278] animate-bounce" style="animation-delay:0.2s"></div>
                        <div class="w-3 h-3 rounded-full bg-[#2b5278] animate-bounce" style="animation-delay:0.4s"></div>
                    </div>
                    <p class="text-[#aab8c2] text-sm">في انتظار تأكيدك من تيليجرام...</p>
                </div>

                <button @click="window.open(`https://t.me/CrepixBot?start=mobile_${requestToken}`, '_blank')"
                    class="w-full bg-[#2b5278] hover:bg-[#3a6a9a] py-3.5 rounded-xl font-semibold flex items-center justify-center gap-2">
                    <span class="text-xl">✈️</span>
                    فتح تيليجرام
                </button>

                <button @click="step = 'phone'; clearInterval(pollTimer)"
                    class="text-[#aab8c2] text-sm">
                    ← تغيير رقم الهاتف
                </button>
            </div>
        </template>

        <!-- Polling starter -->
        <div v-if="step === 'code'" class="hidden" @vue:mounted="onCodeStep"></div>
    </div>
</template>
