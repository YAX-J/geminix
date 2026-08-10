import request from './request'

// 日报提醒接口
export function getTodayReminderStatus() {
  return request.get('/reminders/today-status')
}
