<script setup>
import { ref, onUnmounted } from 'vue';

const emit = defineEmits(['logged-in']);

const step = ref('welcome'); // welcome | phone | waiting
const phone = ref('');
const selectedCountry = ref({ code: '+966', name: 'السعودية', flag: '🇸🇦' });
const showCountry = ref(false);
const error = ref('');
const sending = ref(false);
const requestToken = ref('');
let pollTimer = null;

const countries = [
    { code: '+966', name: 'السعودية', flag: '🇸🇦' },
    { code: '+971', name: 'الإمارات', flag: '🇦🇪' },
    { code: '+965', name: 'الكويت', flag: '🇰🇼' },
    { code: '+974', name: 'قطر', flag: '🇶🇦' },
    { code: '+973', name: 'البحرين', flag: '🇧🇭' },
    { code: '+968', name: 'عُمان', flag: '🇴🇲' },
    { code: '+20',  name: 'مصر', flag: '🇪🇬' },
    { code: '+964', name: 'العراق', flag: '🇮🇶' },
    { code: '+962', name: 'الأردن', flag: '🇯🇴' },
    { code: '+90',  name: 'تركيا', flag: '🇹🇷' },
    { code: '+1',   name: 'أمريكا', flag: '🇺🇸' },
    { code: '+44',  name: 'بريطانيا', flag: '🇬🇧' },
];

async function requestCode() {
    if (!phone.value.trim() || sending.value) return;
    sending.value = true;
    error.value = '';
    try {
        const res = await fetch('https://shoof-tv.net/api/v1/mobile/auth/request', {
            headers: { Accept: 'application/json' }
        });
        const data = await res.json();
        requestToken.value = data.token;
        window.open(data.deep_link, '_blank');
        step.value = 'waiting';
        startPoll();
    } catch {
        error.value = 'فشل الاتصال. تحقق من الإنترنت.';
    } finally { sending.value = false; }
}

function startPoll() {
    pollTimer = setInterval(async () => {
        try {
            const res = await fetch(
                `https://shoof-tv.net/api/v1/mobile/auth/verify/${requestToken.value}`,
                { headers: { Accept: 'application/json' } }
            );
            const data = await res.json();
            if (data.status === 'ok') {
                clearInterval(pollTimer);
                try {
                    const { Preferences } = await import('@capacitor/preferences');
                    await Preferences.set({ key: 'session_token', value: data.session_token });
                } catch {
                    localStorage.setItem('session_token', data.session_token);
                }
                emit('logged-in', data.session_token);
            } else if (data.status === 'expired') {
                clearInterval(pollTimer);
                error.value = 'انتهت المهلة. حاول مجدداً.';
                step.value = 'phone';
            }
        } catch {}
    }, 2000);
}

onUnmounted(() => clearInterval(pollTimer));
</script>

<template>
    <div dir="rtl" class="relative min-h-screen bg-black overflow-hidden flex flex-col">

        <!-- Background cinematic overlay -->
        <div class="absolute inset-0 bg-gradient-to-b from-black/30 via-black/10 to-black z-0"></div>
        <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_#1a0533_0%,_#000_70%)] opacity-70 z-0"></div>

        <!-- Animated background blobs -->
        <div class="absolute top-20 right-10 w-40 h-40 bg-purple-900/30 rounded-full blur-3xl z-0 animate-pulse"></div>
        <div class="absolute top-60 left-5 w-32 h-32 bg-red-900/20 rounded-full blur-3xl z-0 animate-pulse" style="animation-delay:1s"></div>

        <!-- Content -->
        <div class="relative z-10 flex flex-col min-h-screen">

            <!-- Welcome Screen -->
            <div v-if="step === 'welcome'" class="flex flex-col flex-1">

                <!-- Top logo area -->
                <div class="flex-1 flex flex-col items-center justify-center px-6 pt-16 pb-8">
                    <!-- Logo -->
                    <div class="relative mb-8">
                        <div class="w-28 h-28 rounded-3xl bg-gradient-to-br from-purple-600 via-red-600 to-orange-500 flex items-center justify-center shadow-2xl shadow-purple-900/50">
                            <span class="text-5xl">🎬</span>
                        </div>
                        <div class="absolute -bottom-1 -right-1 w-8 h-8 bg-green-500 rounded-full flex items-center justify-center text-xs font-bold border-2 border-black">▶</div>
                    </div>

                    <h1 class="text-4xl font-black text-white mb-2 tracking-tight">شوف <span class="text-red-500">TV</span></h1>
                    <p class="text-gray-400 text-base text-center leading-relaxed">
                        آلاف المسلسلات والأنيميات<br/>في مكان واحد
                    </p>

                    <!-- Preview thumbnails -->
                    <div class="flex gap-2 mt-8 opacity-60">
                        <div class="w-16 h-24 rounded-lg bg-gradient-to-b from-purple-800 to-purple-900"></div>
                        <div class="w-16 h-24 rounded-lg bg-gradient-to-b from-red-800 to-red-900 -mt-2"></div>
                        <div class="w-16 h-24 rounded-lg bg-gradient-to-b from-blue-800 to-blue-900"></div>
                        <div class="w-16 h-24 rounded-lg bg-gradient-to-b from-green-800 to-green-900 -mt-2"></div>
                        <div class="w-16 h-24 rounded-lg bg-gradient-to-b from-orange-800 to-orange-900"></div>
                    </div>
                </div>

                <!-- Bottom CTA -->
                <div class="px-6 pb-12 space-y-3">
                    <button @click="step = 'phone'"
                        class="w-full py-4 rounded-2xl bg-gradient-to-r from-red-600 to-red-700 text-white font-bold text-lg shadow-lg shadow-red-900/50 active:scale-95 transition-transform">
                        ابدأ المشاهدة
                    </button>
                    <p class="text-center text-gray-500 text-xs">
                        يتطلب حساب تيليجرام للدخول
                    </p>
                </div>
            </div>

            <!-- Phone Screen -->
            <div v-else-if="step === 'phone'" class="flex flex-col flex-1 px-6">

                <!-- Back + Header -->
                <div class="pt-12 pb-8">
                    <button @click="step = 'welcome'" class="text-gray-400 text-sm mb-6 flex items-center gap-1">
                        ← رجوع
                    </button>
                    <h2 class="text-3xl font-black text-white">تسجيل الدخول</h2>
                    <p class="text-gray-400 mt-2">أدخل رقمك لتلقّي رمز الدخول</p>
                </div>

                <div class="space-y-3 flex-1">
                    <!-- Country selector -->
                    <div>
                        <button @click="showCountry = !showCountry"
                            class="w-full bg-white/10 border border-white/20 backdrop-blur rounded-2xl px-4 py-4 flex items-center justify-between">
                            <span class="flex items-center gap-3">
                                <span class="text-2xl">{{ selectedCountry.flag }}</span>
                                <div class="text-right">
                                    <div class="text-white font-semibold text-sm">{{ selectedCountry.name }}</div>
                                    <div class="text-gray-400 text-xs">{{ selectedCountry.code }}</div>
                                </div>
                            </span>
                            <span class="text-gray-400 text-xs transition-transform" :class="showCountry ? 'rotate-180' : ''">▼</span>
                        </button>

                        <!-- Dropdown -->
                        <div v-if="showCountry"
                            class="mt-1 bg-gray-900 border border-white/10 rounded-2xl overflow-hidden max-h-48 overflow-y-auto">
                            <button v-for="c in countries" :key="c.code"
                                @click="selectedCountry = c; showCountry = false"
                                class="w-full px-4 py-3.5 flex items-center gap-3 active:bg-white/10 border-b border-white/5 last:border-0">
                                <span class="text-xl">{{ c.flag }}</span>
                                <span class="flex-1 text-white text-sm text-right">{{ c.name }}</span>
                                <span class="text-gray-500 text-xs">{{ c.code }}</span>
                            </button>
                        </div>
                    </div>

                    <!-- Phone input -->
                    <div class="bg-white/10 border border-white/20 backdrop-blur rounded-2xl flex overflow-hidden">
                        <div class="px-4 flex items-center border-l border-white/10">
                            <span class="text-gray-300 font-mono text-sm">{{ selectedCountry.code }}</span>
                        </div>
                        <input v-model="phone" type="tel" inputmode="numeric"
                            placeholder="5XXXXXXXX"
                            class="flex-1 bg-transparent text-white px-4 py-4 text-base outline-none placeholder-gray-600"
                            @keyup.enter="requestCode" />
                    </div>

                    <p v-if="error" class="text-red-400 text-xs text-center bg-red-900/20 rounded-xl py-2 px-3">
                        {{ error }}
                    </p>
                </div>

                <!-- CTA -->
                <div class="pb-12 pt-6">
                    <button @click="requestCode" :disabled="!phone.trim() || sending"
                        class="w-full py-4 rounded-2xl font-bold text-lg transition-all active:scale-95 disabled:opacity-40"
                        :class="phone.trim() ? 'bg-gradient-to-r from-red-600 to-red-700 text-white shadow-lg shadow-red-900/40' : 'bg-white/10 text-gray-500'">
                        {{ sending ? 'جاري الإرسال...' : 'الحصول على الرمز ←' }}
                    </button>
                    <p class="text-center text-gray-600 text-xs mt-3">سيُرسَل رمز التحقق عبر تيليجرام</p>
                </div>
            </div>

            <!-- Waiting Screen -->
            <div v-else-if="step === 'waiting'" class="flex flex-col flex-1 items-center justify-center px-6 gap-8">

                <!-- Animated icon -->
                <div class="relative">
                    <div class="w-28 h-28 rounded-full bg-gradient-to-br from-blue-600 to-blue-800 flex items-center justify-center shadow-2xl">
                        <span class="text-5xl">✈️</span>
                    </div>
                    <div class="absolute inset-0 rounded-full border-4 border-blue-500/30 animate-ping"></div>
                </div>

                <div class="text-center">
                    <h2 class="text-2xl font-black text-white mb-3">تحقق من تيليجرام</h2>
                    <p class="text-gray-400 text-base leading-relaxed">
                        افتح بوت شوف في تيليجرام<br/>واضغط <span class="text-white font-bold bg-white/10 px-2 py-0.5 rounded">Start</span> للدخول فوراً
                    </p>
                </div>

                <!-- Pulse dots -->
                <div class="flex gap-2">
                    <div v-for="i in 3" :key="i"
                        class="w-3 h-3 rounded-full bg-blue-500 animate-bounce"
                        :style="`animation-delay: ${(i-1)*0.2}s`"></div>
                </div>

                <button @click="window.open(`https://t.me/CrepixBot?start=mobile_${requestToken}`, '_blank')"
                    class="w-full py-4 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-base flex items-center justify-center gap-3 active:scale-95 transition-transform">
                    <span class="text-xl">✈️</span>
                    فتح تيليجرام
                </button>

                <button @click="step = 'phone'; clearInterval(pollTimer)" class="text-gray-500 text-sm">
                    ← تغيير الرقم
                </button>
            </div>

        </div>
    </div>
</template>
