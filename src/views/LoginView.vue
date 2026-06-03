<script setup>
import { ref, onUnmounted, nextTick } from 'vue';

const emit = defineEmits(['logged-in']);

// steps: welcome → phone → sent → code → loading
const step = ref('welcome');
const phone = ref('');
const otp = ref(['', '', '', '', '', '']);
const selectedCountry = ref({ code: '+966', name: 'السعودية', flag: '🇸🇦' });
const showCountry = ref(false);
const error = ref('');
const sending = ref(false);
const verifying = ref(false);
const authToken = ref('');
let pollTimer = null;
const otpInputs = ref([]);

const countries = [
    { code: '+1',   name: 'أمريكا', flag: '🇺🇸' },
    { code: '+44',  name: 'بريطانيا', flag: '🇬🇧' },
    { code: '+20',  name: 'مصر', flag: '🇪🇬' },
    { code: '+90',  name: 'تركيا', flag: '🇹🇷' },
    { code: '+7',   name: 'روسيا', flag: '🇷🇺' },
    { code: '+49',  name: 'ألمانيا', flag: '🇩🇪' },
    { code: '+33',  name: 'فرنسا', flag: '🇫🇷' },
    { code: '+39',  name: 'إيطاليا', flag: '🇮🇹' },
    { code: '+34',  name: 'إسبانيا', flag: '🇪🇸' },
    { code: '+91',  name: 'الهند', flag: '🇮🇳' },
    { code: '+92',  name: 'باكستان', flag: '🇵🇰' },
    { code: '+880', name: 'بنغلاديش', flag: '🇧🇩' },
    { code: '+62',  name: 'إندونيسيا', flag: '🇮🇩' },
    { code: '+966', name: 'السعودية', flag: '🇸🇦' },
    { code: '+971', name: 'الإمارات', flag: '🇦🇪' },
    { code: '+965', name: 'الكويت', flag: '🇰🇼' },
    { code: '+974', name: 'قطر', flag: '🇶🇦' },
    { code: '+973', name: 'البحرين', flag: '🇧🇭' },
    { code: '+968', name: 'عُمان', flag: '🇴🇲' },
    { code: '+962', name: 'الأردن', flag: '🇯🇴' },
    { code: '+961', name: 'لبنان', flag: '🇱🇧' },
    { code: '+964', name: 'العراق', flag: '🇮🇶' },
    { code: '+963', name: 'سوريا', flag: '🇸🇾' },
    { code: '+967', name: 'اليمن', flag: '🇾🇪' },
    { code: '+218', name: 'ليبيا', flag: '🇱🇾' },
    { code: '+213', name: 'الجزائر', flag: '🇩🇿' },
    { code: '+216', name: 'تونس', flag: '🇹🇳' },
    { code: '+212', name: 'المغرب', flag: '🇲🇦' },
    { code: '+249', name: 'السودان', flag: '🇸🇩' },
    { code: '+251', name: 'إثيوبيا', flag: '🇪🇹' },
    { code: '+234', name: 'نيجيريا', flag: '🇳🇬' },
    { code: '+254', name: 'كينيا', flag: '🇰🇪' },
    { code: '+27',  name: 'جنوب أفريقيا', flag: '🇿🇦' },
    { code: '+55',  name: 'البرازيل', flag: '🇧🇷' },
    { code: '+52',  name: 'المكسيك', flag: '🇲🇽' },
    { code: '+54',  name: 'الأرجنتين', flag: '🇦🇷' },
    { code: '+57',  name: 'كولومبيا', flag: '🇨🇴' },
    { code: '+86',  name: 'الصين', flag: '🇨🇳' },
    { code: '+81',  name: 'اليابان', flag: '🇯🇵' },
    { code: '+82',  name: 'كوريا الجنوبية', flag: '🇰🇷' },
    { code: '+66',  name: 'تايلاند', flag: '🇹🇭' },
    { code: '+84',  name: 'فيتنام', flag: '🇻🇳' },
    { code: '+60',  name: 'ماليزيا', flag: '🇲🇾' },
    { code: '+63',  name: 'الفلبين', flag: '🇵🇭' },
    { code: '+65',  name: 'سنغافورة', flag: '🇸🇬' },
    { code: '+61',  name: 'أستراليا', flag: '🇦🇺' },
    { code: '+64',  name: 'نيوزيلندا', flag: '🇳🇿' },
    { code: '+31',  name: 'هولندا', flag: '🇳🇱' },
    { code: '+32',  name: 'بلجيكا', flag: '🇧🇪' },
    { code: '+41',  name: 'سويسرا', flag: '🇨🇭' },
    { code: '+43',  name: 'النمسا', flag: '🇦🇹' },
    { code: '+46',  name: 'السويد', flag: '🇸🇪' },
    { code: '+47',  name: 'النرويج', flag: '🇳🇴' },
    { code: '+45',  name: 'الدنمارك', flag: '🇩🇰' },
    { code: '+358', name: 'فنلندا', flag: '🇫🇮' },
    { code: '+48',  name: 'بولندا', flag: '🇵🇱' },
    { code: '+380', name: 'أوكرانيا', flag: '🇺🇦' },
    { code: '+30',  name: 'اليونان', flag: '🇬🇷' },
    { code: '+351', name: 'البرتغال', flag: '🇵🇹' },
    { code: '+98',  name: 'إيران', flag: '🇮🇷' },
    { code: '+93',  name: 'أفغانستان', flag: '🇦🇫' },
    { code: '+94',  name: 'سريلانكا', flag: '🇱🇰' },
    { code: '+95',  name: 'ميانمار', flag: '🇲🇲' },
];

async function sendCode() {
    if (!phone.value.trim() || sending.value) return;
    sending.value = true;
    error.value = '';
    try {
        const res = await fetch('https://shoof-tv.net/api/v1/mobile/auth/request', {
            method: 'POST',
            headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone: selectedCountry.value.code + phone.value })
        });
        const data = await res.json();
        authToken.value = data.token;
        window.open(data.deep_link, '_blank');
        step.value = 'sent';
        startStatusPoll();
    } catch {
        error.value = 'فشل الإرسال. تحقق من الإنترنت.';
    } finally { sending.value = false; }
}

function startStatusPoll() {
    pollTimer = setInterval(async () => {
        try {
            const res = await fetch(
                `https://shoof-tv.net/api/v1/mobile/auth/status/${authToken.value}`,
                { headers: { Accept: 'application/json' } }
            );
            const data = await res.json();
            if (data.status === 'claimed') {
                clearInterval(pollTimer);
                step.value = 'code';
                await nextTick();
                otpInputs.value[0]?.focus();
            } else if (data.status === 'expired') {
                clearInterval(pollTimer);
                error.value = 'انتهت المهلة.';
                step.value = 'phone';
            }
        } catch {}
    }, 2000);
}

function onOtpInput(i, e) {
    const val = e.target.value.replace(/\D/g, '').slice(-1);
    otp.value[i] = val;
    if (val && i < 5) otpInputs.value[i + 1]?.focus();
    if (otp.value.join('').length === 6) submitOtp();
}

function onOtpKeydown(i, e) {
    if (e.key === 'Backspace' && !otp.value[i] && i > 0) {
        otpInputs.value[i - 1]?.focus();
    }
}

async function submitOtp() {
    const code = otp.value.join('');
    if (code.length !== 6 || verifying.value) return;
    verifying.value = true;
    error.value = '';
    try {
        const res = await fetch('https://shoof-tv.net/api/v1/mobile/auth/verify-otp', {
            method: 'POST',
            headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
            body: JSON.stringify({ token: authToken.value, otp_code: code })
        });
        const data = await res.json();
        if (data.status === 'ok') {
            step.value = 'loading';
            try {
                const { Preferences } = await import('@capacitor/preferences');
                await Preferences.set({ key: 'session_token', value: data.session_token });
            } catch { localStorage.setItem('session_token', data.session_token); }
            setTimeout(() => emit('logged-in', data.session_token), 600);
        } else {
            error.value = data.message || 'الرمز غير صحيح';
            otp.value = ['','','','','',''];
            otpInputs.value[0]?.focus();
        }
    } catch { error.value = 'خطأ في الاتصال'; }
    finally { verifying.value = false; }
}

onUnmounted(() => clearInterval(pollTimer));
</script>

<template>
<div dir="rtl" class="relative min-h-screen bg-black text-white overflow-hidden flex flex-col select-none">

    <!-- BG gradient -->
    <div class="absolute inset-0 bg-gradient-to-b from-purple-950/40 via-black to-black pointer-events-none"></div>
    <div class="absolute top-0 inset-x-0 h-64 bg-gradient-to-b from-red-950/20 to-transparent pointer-events-none"></div>

    <!-- Loading -->
    <div v-if="step === 'loading'" class="relative z-10 flex-1 flex flex-col items-center justify-center gap-4">
        <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-red-600 to-red-800 flex items-center justify-center text-4xl">🎬</div>
        <div class="flex gap-1.5 mt-2">
            <div v-for="i in 3" :key="i" class="w-2 h-2 rounded-full bg-red-500 animate-bounce" :style="`animation-delay:${(i-1)*0.15}s`"></div>
        </div>
        <p class="text-gray-400 text-sm">جاري الدخول...</p>
    </div>

    <!-- Welcome -->
    <div v-else-if="step === 'welcome'" class="relative z-10 flex flex-col flex-1">
        <div class="flex-1 flex flex-col items-center justify-center px-8 gap-6 pt-16">
            <div class="relative">
                <div class="w-32 h-32 rounded-3xl bg-gradient-to-br from-red-600 via-red-700 to-red-900 flex items-center justify-center shadow-2xl shadow-red-900/60">
                    <span class="text-6xl">🎬</span>
                </div>
                <div class="absolute -bottom-2 -left-2 bg-green-500 w-8 h-8 rounded-full flex items-center justify-center border-2 border-black text-sm font-bold">▶</div>
            </div>
            <div class="text-center">
                <h1 class="text-4xl font-black tracking-tight">شوف <span class="text-red-500">TV</span></h1>
                <p class="text-gray-400 mt-2 text-sm leading-relaxed">شاهد آلاف المسلسلات والأنيميات<br/>بجودة عالية في أي مكان</p>
            </div>
            <div class="flex gap-3 mt-2">
                <div v-for="c in ['from-purple-800','from-blue-800','from-red-800','from-green-800','from-orange-800']"
                    :key="c" :class="`w-14 h-20 rounded-xl bg-gradient-to-b ${c} to-black/80 opacity-70`"></div>
            </div>
        </div>
        <div class="px-6 pb-12 space-y-3">
            <button @click="step='phone'" class="w-full py-4 rounded-2xl bg-red-600 font-bold text-lg active:scale-95 transition-transform shadow-lg shadow-red-900/40">
                ابدأ المشاهدة مجاناً
            </button>
            <p class="text-center text-gray-600 text-xs">يتطلب حساب تيليجرام</p>
        </div>
    </div>

    <!-- Phone -->
    <div v-else-if="step === 'phone'" class="relative z-10 flex flex-col flex-1 px-6">
        <div class="pt-12 pb-6">
            <button @click="step='welcome'" class="text-gray-500 text-sm flex items-center gap-1 mb-6">← رجوع</button>
            <h2 class="text-3xl font-black">أدخل رقمك</h2>
            <p class="text-gray-500 mt-1 text-sm">سنرسل رمز التحقق عبر تيليجرام</p>
        </div>

        <div class="space-y-3 flex-1">
            <!-- Country -->
            <button @click="showCountry=!showCountry"
                class="w-full bg-white/8 border border-white/12 rounded-2xl px-4 py-3.5 flex items-center gap-3">
                <span class="text-2xl">{{ selectedCountry.flag }}</span>
                <div class="flex-1 text-right">
                    <div class="text-white text-sm font-medium">{{ selectedCountry.name }}</div>
                    <div class="text-gray-500 text-xs">{{ selectedCountry.code }}</div>
                </div>
                <span class="text-gray-600 text-xs" :class="showCountry?'rotate-180':''">▼</span>
            </button>

            <div v-if="showCountry" class="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden max-h-52 overflow-y-auto">
                <button v-for="c in countries" :key="c.code" @click="selectedCountry=c; showCountry=false"
                    class="w-full px-4 py-3 flex items-center gap-3 active:bg-white/8 border-b border-white/5 last:border-0">
                    <span class="text-xl">{{ c.flag }}</span>
                    <span class="flex-1 text-sm text-right text-white">{{ c.name }}</span>
                    <span class="text-gray-500 text-xs font-mono">{{ c.code }}</span>
                </button>
            </div>

            <!-- Phone input -->
            <div class="bg-white/8 border border-white/12 rounded-2xl flex overflow-hidden focus-within:border-red-500/50 transition-colors">
                <span class="px-4 flex items-center text-gray-400 text-sm font-mono border-l border-white/10">{{ selectedCountry.code }}</span>
                <input v-model="phone" type="tel" inputmode="numeric" placeholder="5XXXXXXXX"
                    class="flex-1 bg-transparent text-white px-4 py-4 outline-none placeholder-gray-700 text-base"
                    @keyup.enter="sendCode" />
            </div>

            <p v-if="error" class="bg-red-900/30 border border-red-800/50 rounded-xl px-4 py-2.5 text-red-400 text-xs">{{ error }}</p>
        </div>

        <div class="pb-12 pt-6">
            <button @click="sendCode" :disabled="!phone.trim()||sending"
                class="w-full py-4 rounded-2xl font-bold text-base transition-all active:scale-95 disabled:opacity-40"
                :class="phone.trim()?'bg-red-600 text-white shadow-lg shadow-red-900/30':'bg-white/10 text-gray-600'">
                {{ sending ? '...' : 'التالي ←' }}
            </button>
        </div>
    </div>

    <!-- Sent / waiting for Telegram -->
    <div v-else-if="step === 'sent'" class="relative z-10 flex flex-col flex-1 items-center justify-center px-6 gap-6">
        <div class="relative">
            <div class="w-24 h-24 rounded-full bg-blue-600 flex items-center justify-center text-5xl shadow-xl shadow-blue-900/50">✈️</div>
            <div class="absolute inset-0 rounded-full border-2 border-blue-500/40 animate-ping"></div>
        </div>
        <div class="text-center">
            <h2 class="text-2xl font-black">افتح تيليجرام</h2>
            <p class="text-gray-400 mt-2 text-sm leading-relaxed">اضغط <span class="bg-white/10 text-white px-2 py-0.5 rounded font-bold">Start</span> في بوت شوف<br/>وسيصلك رمز التحقق فوراً</p>
        </div>
        <div class="flex gap-2">
            <div v-for="i in 3" :key="i" class="w-2.5 h-2.5 rounded-full bg-blue-500 animate-bounce" :style="`animation-delay:${(i-1)*0.2}s`"></div>
        </div>
        <button @click="window.open(`https://t.me/CrepixBot?start=mobile_${authToken}`, '_blank')"
            class="w-full py-4 rounded-2xl bg-blue-600 font-bold text-base flex items-center justify-center gap-2 active:scale-95 transition-transform">
            <span>✈️</span> فتح تيليجرام
        </button>
        <button @click="step='phone'; clearInterval(pollTimer)" class="text-gray-600 text-sm">← رجوع</button>
    </div>

    <!-- OTP code entry -->
    <div v-else-if="step === 'code'" class="relative z-10 flex flex-col flex-1 px-6">
        <div class="pt-12 pb-8">
            <button @click="step='sent'" class="text-gray-500 text-sm mb-6 flex items-center gap-1">← رجوع</button>
            <div class="w-14 h-14 rounded-2xl bg-green-600/20 border border-green-600/40 flex items-center justify-center text-2xl mb-4">✅</div>
            <h2 class="text-3xl font-black">أدخل الرمز</h2>
            <p class="text-gray-500 mt-1 text-sm">وصلك رمز مكون من 6 أرقام عبر تيليجرام</p>
        </div>

        <!-- OTP boxes -->
        <div class="flex gap-3 justify-center mb-6" dir="ltr">
            <input v-for="(_, i) in 6" :key="i" :ref="el => otpInputs[i] = el"
                v-model="otp[i]" type="tel" inputmode="numeric" maxlength="1"
                @input="onOtpInput(i, $event)"
                @keydown="onOtpKeydown(i, $event)"
                class="w-12 h-14 text-center text-xl font-bold rounded-xl border-2 bg-white/8 outline-none transition-colors"
                :class="otp[i] ? 'border-red-500 text-white' : 'border-white/15 text-white'" />
        </div>

        <p v-if="error" class="bg-red-900/30 border border-red-800/50 rounded-xl px-4 py-2.5 text-red-400 text-xs text-center mb-4">{{ error }}</p>

        <button @click="submitOtp" :disabled="otp.join('').length!==6||verifying"
            class="w-full py-4 rounded-2xl font-bold text-base active:scale-95 transition-all disabled:opacity-40"
            :class="otp.join('').length===6 ? 'bg-red-600 text-white' : 'bg-white/10 text-gray-600'">
            {{ verifying ? '...' : 'تحقق ←' }}
        </button>

        <button @click="step='phone'; otp=['','','','','','']; clearInterval(pollTimer)" class="mt-4 text-center text-gray-600 text-sm w-full">
            إعادة إرسال الرمز
        </button>
    </div>

</div>
</template>
