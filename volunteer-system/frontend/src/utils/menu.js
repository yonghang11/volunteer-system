export const ROLE_MAP = {
  STUDENT: '学生',
  MENTOR: '导师',
  LAB_ADMIN: '实训室管理员',
  COLLEGE_ADMIN: '学院管理员'
}

export const HOME_BY_ROLE = {
  STUDENT: '/student/home',
  MENTOR: '/mentor/review',
  LAB_ADMIN: '/lab/manage',
  COLLEGE_ADMIN: '/admin'
}

export const MENU_BY_ROLE = {
  STUDENT: [
    { path: '/student/home', label: '学生首页', icon: 'HomeFilled' },
    { path: '/student/intent', label: '意向填报', icon: 'EditPen' },
    { path: '/student/interview', label: '面试安排', icon: 'Calendar' },
    { path: '/student/result', label: '录取结果', icon: 'Trophy' }
  ],
  MENTOR: [
    { path: '/mentor/review', label: '面试评审', icon: 'DocumentChecked' }
  ],
  LAB_ADMIN: [
    { path: '/lab/manage', label: '实训室管理', icon: 'OfficeBuilding' }
  ],
  COLLEGE_ADMIN: [
    { path: '/admin', label: '学院管理', icon: 'Setting' }
  ]
}
