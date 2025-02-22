export interface User {
  id: number
  username: string
  email: string
  avatar?: string
  createTime: string
}

export interface AuthState {
  currentUser: User | null
  sessionId: string | null
  isAdmin: boolean
}

export interface Post {
  id: number
  title: string
  content: string
  createTime: string
  updateTime: string
  userId: number
  user: User
  imageUrls: string[]
  viewCount: number
  commentCount: number
  likeCount: number
  collectCount: number
  status: string
}

export interface Comment {
  id: number
  content: string
  createTime: string
  userId: number
  user: User
  postId: number
  post: Post
} 