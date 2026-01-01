<template>
  <div class="admin-user-container">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="fetchUsers">刷新列表</el-button>
    </div>

    <el-card>
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="role" label="角色" width="100">
             <template #default="scope">
                <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'success'">{{ scope.row.role }}</el-tag>
             </template>
        </el-table-column>
        <el-table-column prop="memberLevel" label="会员等级" width="120">
             <template #default="scope">
                <el-tag :type="scope.row.memberLevel === 1 ? 'warning' : 'info'">
                    {{ scope.row.memberLevel === 1 ? 'VIP会员' : '普通用户' }}
                </el-tag>
             </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleEdit(scope.row)">
              修改等级
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            v-model:current-page="currentPage"
            @current-change="handlePageChange"
          />
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="修改会员等级" width="400px">
        <el-form :model="editForm">
            <el-form-item label="用户名">
                <el-input v-model="editForm.nickname" disabled />
            </el-form-item>
            <el-form-item label="会员等级">
                <el-select v-model="editForm.memberLevel" placeholder="请选择">
                    <el-option label="普通用户" :value="0" />
                    <el-option label="VIP会员" :value="1" />
                </el-select>
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveEdit" :loading="saving">确定</el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

interface User {
    id: number
    username: string
    nickname: string
    role: string
    memberLevel: number
}

const tableData = ref<User[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0) // Default 0

const dialogVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
    id: 0,
    nickname: '',
    memberLevel: 0
})

const fetchUsers = async () => {
    loading.value = true
    try {
        const res: any = await request.get('/admin/users', {
            params: {
                page: currentPage.value,
                size: pageSize.value
            }
        })
        if (res.code === 200) {
            tableData.value = res.data.records
            total.value = res.data.total
        }
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const handlePageChange = (page: number) => {
    currentPage.value = page
    fetchUsers()
}

const handleEdit = (row: User) => {
    editForm.id = row.id
    editForm.nickname = row.nickname
    editForm.memberLevel = row.memberLevel
    dialogVisible.value = true
}

const saveEdit = async () => {
    saving.value = true
    try {
        const res: any = await request.put(`/admin/users/${editForm.id}/member-level`, null, {
            params: { level: editForm.memberLevel }
        })
        if (res.code === 200) {
            ElMessage.success('更新成功')
            dialogVisible.value = false
            fetchUsers()
        } else {
            ElMessage.error(res.msg || '更新失败')
        }
    } catch (e) {
        console.error(e)
    } finally {
        saving.value = false
    }
}

onMounted(() => {
    fetchUsers()
})
</script>

<style scoped>
.admin-user-container {
    padding: 20px;
}
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}
.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}
</style>
