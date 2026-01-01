import request from './request'

// Match backend AuthRequest DTO
export interface LoginData {
    username: string
    password: string
}

// Match backend Result<Map<String, Object>> structure
export interface LoginResponse {
    code: number
    msg: string
    data: {
        token: string
        user: any
    }
}

export const login = (data: LoginData) => {
    return request.post<any, LoginResponse>('/auth/login', data)
}

export interface RegisterData {
    username: string
    password: string
    nickname: string
    gender: number
}

export const register = (data: RegisterData) => {
    return request.post<any, any>('/auth/register', data)
}
