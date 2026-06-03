<script setup>
import { ref, onMounted } from 'vue';
import { api } from '../api.js';
import { useRouter } from 'vue-router';

const props = defineProps(['uuid']);
const router = useRouter();
const file = ref(null);
const items = ref([]);
const loading = ref(true);
const playerUrl = ref('');
const playerOpen = ref(false);
const playerName = ref('');
const videoEl = ref(null);

onMounted(async () => {
    try {
        const data = await api.file(props.uuid);
        file.value = data.file;
        items.value = (data.items || []).sort((a,b) => (a.sort_order||0)-(b.sort_order||0));
    } catch {} finally { loading.value = false; }
});

async function playEpisode(item) {
    if (item.is_locked) return;
    if (!item.has_access) return;
    try {
        const data = await api.mediaUrl(item.uuid);
        playerUrl.value = data.url;
        playerName.value = item.name;
        playerOpen.value = true;
    } catch {}
}

function closePlayer() {
    playerOpen.value = false;
    playerUrl.value = '';
    if (videoEl.value) videoEl.value.pause();
}
</script>

<template>
    <div class="min-h-screen bg-gray-900 text-white pb-6">
        <!-- Header -->
        <header class="sticky top-0 z-20 bg-gray-900/95 backdrop-blur px-4 py-3 flex items-center gap-3 border-b border-gray-800">
            <button @click="router.back()" class="text-2xl">‹</button>
            <h1 class="font-bold truncate flex-1">{{ file?.name || '...' }}</h1>
        </header>

        <div v-if="loading" class="flex items-center justify-center py-20">
            <div class="w-10 h-10 border-4 border-purple-500 border-t-transparent rounded-full animate-spin"></div>
        </div>

        <template v-else-if="file">
            <!-- غلاف + وصف -->
            <div class="relative">
                <img v-if="file.cover_url" :src="file.cover_url" class="w-full h-52 object-cover" />
                <div class="absolute inset-0 bg-gradient-to-t from-gray-900 via-gray-900/60 to-transparent"></div>
                <div class="absolute bottom-0 p-4">
                    <h2 class="font-bold text-xl">{{ file.name }}</h2>
                    <p v-if="file.description" class="text-gray-300 text-sm mt-1 line-clamp-2">{{ file.description }}</p>
                </div>
            </div>

            <!-- الحلقات -->
            <div class="p-4 space-y-3">
                <h3 class="font-bold text-sm text-gray-400">الحلقات ({{ items.filter(i => !i.is_locked).length }})</h3>

                <div v-for="item in items" :key="item.uuid"
                    @click="playEpisode(item)"
                    :class="['rounded-2xl overflow-hidden', item.is_locked ? 'opacity-80 select-none' : 'cursor-pointer active:opacity-70']">
                    <div class="relative">
                        <img v-if="item.thumbnail_url" :src="item.thumbnail_url"
                            class="w-full aspect-video object-cover bg-gray-800" loading="lazy" />
                        <div v-else class="w-full aspect-video bg-gray-800 flex items-center justify-center text-4xl">🎬</div>
                        <div class="absolute inset-0 bg-black/40"></div>
                        <div v-if="item.is_locked" class="absolute inset-0 bg-black/55 flex flex-col items-center justify-center gap-2">
                            <span class="text-4xl">🔒</span>
                            <span v-if="item.locked_until" class="bg-black/70 text-amber-400 text-xs px-3 py-1 rounded-full">
                                متاحة في {{ item.locked_until }}
                            </span>
                        </div>
                        <div v-else class="absolute inset-0 flex items-center justify-center">
                            <div class="w-12 h-12 rounded-full bg-black/60 flex items-center justify-center text-2xl">▶️</div>
                        </div>
                    </div>
                    <div class="bg-gray-800 p-3 flex items-center justify-between">
                        <span class="font-semibold text-sm">🎬 {{ item.name }}</span>
                        <span v-if="item.is_locked" class="text-amber-400 text-xs">🔒 قريباً</span>
                    </div>
                </div>
            </div>
        </template>

        <!-- مشغل الفيديو -->
        <div v-if="playerOpen" class="fixed inset-0 z-50 bg-black flex flex-col">
            <div class="flex items-center gap-2 p-3 bg-black/80">
                <button @click="closePlayer" class="text-3xl text-white">‹</button>
                <span class="text-white font-bold truncate">{{ playerName }}</span>
            </div>
            <video ref="videoEl" :src="playerUrl" controls autoplay playsinline
                class="flex-1 w-full object-contain"></video>
        </div>
    </div>
</template>
