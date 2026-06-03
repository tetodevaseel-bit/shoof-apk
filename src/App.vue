<script setup>
import { ref } from 'vue';
import { setToken } from './api.js';
import LoginView from './views/LoginView.vue';

const props = defineProps(['initialToken']);
const isLoggedIn = ref(!!props.initialToken);

function onLoggedIn(user) {
    isLoggedIn.value = true;
}
</script>

<template>
    <LoginView v-if="!isLoggedIn" @logged-in="onLoggedIn" />
    <router-view v-else />
</template>
