<template>
  <div>
    <a-alert :message="$t('tip.btnPerAlert')"></a-alert>
    <a-divider></a-divider>
    <a-button v-has="['user']">普通用户专属按钮</a-button>
    <a-button v-has="['admin']" style="margin: 0 10px" danger> 管理员专属按钮 </a-button>
    <a-button v-has="['admin', 'user']" type="primary"> 用户和管理员都拥有的按钮 </a-button>
  </div>
</template>
