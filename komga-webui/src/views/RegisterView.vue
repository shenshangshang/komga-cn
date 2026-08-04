<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="5" lg="4">
        <v-card class="pa-6">
          <v-card-title>注册账号</v-card-title>
          <v-card-text v-if="mode === 'DISABLED'">管理员尚未开放注册。</v-card-text>
          <v-form v-else @submit.prevent="register">
            <v-text-field v-model="email" label="邮箱" type="email" autocomplete="email" required />
            <v-text-field v-model="password" label="密码（至少 8 位）" type="password" autocomplete="new-password" required />
            <v-text-field v-if="mode === 'INVITE'" v-model="token" label="邀请码" required />
            <v-alert v-if="error" type="error" text>{{ error }}</v-alert>
            <v-btn color="primary" type="submit" :loading="submitting" block>注册</v-btn>
          </v-form>
          <v-card-actions><v-btn text :to="{name: 'login'}">返回登录</v-btn></v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'RegisterView',
  data: () => ({
    mode: 'DISABLED',
    email: '',
    password: '',
    token: '',
    error: '',
    submitting: false,
  }),
  async mounted() {
    this.token = this.$route.query.token?.toString() || ''
    try {
      this.mode = (await this.$http.get('/api/v1/registration')).data.mode
    } catch (e) {
      this.error = '无法读取注册状态'
    }
  },
  methods: {
    async register() {
      if (!this.email || this.password.length < 8 || (this.mode === 'INVITE' && !this.token)) {
        this.error = '请完整填写注册信息'
        return
      }
      this.submitting = true
      this.error = ''
      try {
        await this.$http.post('/api/v1/registration', {
          email: this.email,
          password: this.password,
          invitationToken: this.token || undefined,
        })
        await this.$router.push({name: 'login', query: {registered: '1'}})
      } catch (e) {
        this.error = e.response?.data?.message || '注册失败'
      } finally {
        this.submitting = false
      }
    },
  },
})
</script>
