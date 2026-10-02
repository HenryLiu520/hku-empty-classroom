/**
 * 界面文案：英文 / 简体中文 / 繁體中文
 * 三种语言必须保持同样的 key（缺 key 会回退到英文）。
 */
export default {
  en: {
    app: { name: 'Empty Classroom' },
    nav: {
      home: 'Home', find: 'Find a room', rooms: 'All rooms',
      manage: 'Change room use time', signOut: 'Sign out', accounts: 'Accounts'
    },
    lang: { label: 'Language' },
    roles: {
      user: 'Student (read only)', admin: 'Administrator', superadmin: 'Super administrator'
    },
    accounts: {
      title: 'Accounts', subtitle: 'only a super administrator can open this page',
      roleHint: 'Roles are layered: a student can read; an administrator can also lock or release '
        + 'room time; a super administrator can also manage accounts, including promoting and '
        + 'demoting them.',
      newAccount: 'New account', username: 'Username', displayName: 'Display name',
      email: 'HKU email', emailOptional: 'Optional, but must end in hku.hk',
      role: 'Role', actions: 'Actions', changeRole: 'Change role',
      resetPassword: 'Reset password', newPassword: 'New password',
      created: 'Account created', roleChanged: 'Role updated', passwordChanged: 'Password updated',
      deleted: 'Account deleted', loadFailed: 'Could not load the accounts',
      saveFailed: 'Could not save the account', deleteFailed: 'Could not delete the account'
    },
    common: {
      date: 'Date', refresh: 'Refresh', save: 'Save', cancel: 'Cancel',
      edit: 'Edit', delete: 'Delete', yes: 'Yes', no: 'No', none: '—', floor: 'Floor'
    },
    login: {
      tagline: 'See which teaching rooms are free between classes',
      username: 'Username', usernameOrEmail: 'HKU email or username',
      email: 'HKU email, e.g. name@connect.hku.hk', password: 'Password',
      signIn: 'Sign in', createAccount: 'Create account',
      hint: 'Sign-up is for HKU addresses only: the email has to end in hku.hk '
        + '(@connect.hku.hk, @hku.hk, ...). This prototype sends no verification email.',
      demo: 'Demo accounts',
      createLink: 'Create an account with your HKU email',
      backLink: 'Back to sign in',
      signedIn: 'Signed in as {name}', created: 'Account created. Signed in as {name}',
      errSignIn: 'Sign in failed', errSignUp: 'Sign-up failed'
    },
    find: {
      building: 'Building', allBuildings: 'All buildings',
      from: 'From', to: 'To', search: 'Search', reset: 'Reset', sortBy: 'Sort by',
      showAll: 'Show all rooms',
      onlyFree: 'Only rooms free for the whole interval are listed.',
      matched: '{matched} of {total} rooms match',
      listingMatching: 'listing matching rooms only', listingEvery: 'listing every room',
      freeStops: 'a free window stops 10 minutes before the next class, so there is time to '
        + 'pack up and leave. That is why rooms are offered until :50.',
      colRoom: 'Room', colLocation: 'Location', colSeats: 'Seats', colType: 'Type',
      colStatus: 'Status', colWindow: 'Free window', colNext: 'Next class',
      after: 'after {time}', noWindowLong: 'no window long enough',
      empty: "No room is free for that whole interval. Try a shorter interval, another "
        + "building, or turn on 'Show all rooms'.",
      noticePermission: 'Please note: a room must be given up if a class or a member of staff '
        + 'needs it, and a member of staff may ask you to leave. This page shows where rooms are '
        + 'free. It does not grant permission to use a room.',
      noticeShared: 'A room shown as free may already be used by other students, and rooms are '
        + 'shared. Free means the room is not timetabled or taken, not that it is empty.',
      timelineTitle: 'Room timeline: {room}, {date}',
      legendInUse: 'In use (class or posted change, whole hours)', legendFree: 'Free',
      sortWindow: 'Longest free window', sortCode: 'Room code (A to Z)',
      sortBuilding: 'Building, then room code', sortSeats: 'Most seats first',
      statusAvailable: 'Available', statusLater: 'Free later', statusInUse: 'In use',
      statusNone: 'No window',
      noTimelineHint: 'Click a row to see that room’s day.'
    },
    rooms: {
      title: 'All rooms', subtitle: 'every room, not filtered by time',
      filterPlaceholder: 'Filter room code or building', noMatch: 'No room matches that filter.',
      roomLabel: 'Room',
      attributesHint: 'attributes come from the room record; the timetable is computed from the '
        + 'timetable plus reported changes',
      facilities: 'Facilities', lastVerified: 'last verified {when}',
      sampleData: 'sample data, not verified yet',
      seatsTotal: 'Seats total', sockets: 'Sockets', seatType: 'Seat type',
      seatTypePlaceholder: 'Fixed rows / Long shared table / Individual desks',
      saveHint: 'Saving stamps today as the verified date and is recorded in the audit log.',
      timetable: 'Timetable',
      inUseTip: 'In use {from} – {to}', freeTip: 'Free {from} – {to}',
      noteAllFree: 'No classes and no posted changes on this day — free all day.',
      noteInUse: 'In use {ranges}',
      reviews: 'Student reviews', reviewCount: 'no reviews | 1 review | {n} reviews',
      ratingFrom: '★ {avg} from {n} review | ★ {avg} from {n} reviews',
      yourRating: 'Your rating', yourReview: 'Your review',
      reviewPlaceholder: 'What is it like to study here? Noise, sockets, seating, light…',
      postReview: 'Post your review', updateReview: 'Update your review',
      deleteYours: 'Delete yours',
      confirmDeleteOthers: 'Delete this review? It was written by another user.',
      reviewHint: 'One review per person per room. Please review the room, not the people in it.',
      newest: 'Newest first', highest: 'Highest rated',
      noReviews: 'No reviews yet. Be the first to describe this room.',
      pickRoom: 'Pick a room on the left to see its details.',
      facilitiesSaved: 'Facilities saved and stamped as verified today',
      reviewSaved: 'Review saved', reviewDeleted: 'Review deleted',
      needRating: 'Please give a rating from 1 to 5 stars',
      needBody: 'Please write a few words as well',
      reviewFailed: 'Could not save the review', reviewDeleteFailed: 'Could not delete the review',
      reviewsFailed: 'Could not load reviews: {status}',
      facilitiesFailed: 'Could not save the facilities',
      timelineFailed: 'Could not load the timetable for this room: {status}'
    },
    manage: {
      subtitle: 'Managers may add or release the use time of a room. Every change is logged '
        + 'and expires on its own.',
      room: 'Room', chooseRoom: 'Choose a room', whatHappens: 'What happens',
      addUse: 'Add use', release: 'Release',
      addUseHint: 'takes free hours away, for example an event needs the room.',
      releaseHint: 'gives hours back, for example a class is cancelled.',
      date: 'Date', fromHour: 'From (hour)', toHour: 'To (:50)',
      reason: 'Reason', reasonPlaceholder: 'Department meeting, about 25 people',
      expiresAt: 'Expires at',
      expiresPlaceholder: 'Leave empty to keep until someone cancels it',
      submit: 'Submit change',
      tabMine: 'My changes', tabAll: 'All changes from every manager', tabAudit: 'Audit log',
      colChange: 'Change', colInterval: 'Interval', colState: 'State', colAction: 'Action',
      colSubmittedBy: 'Submitted by', colWhen: 'When', colActor: 'Actor', colDetail: 'Detail',
      allManagersHint: 'Every change is listed here so that work can be handed over. You can '
        + 'cancel your own changes; cancelling another administrator\'s change is a super '
        + 'administrator action. Nothing is deleted: cancelling only makes a change inactive.',
      onlySuperCanCancel: 'Only a super administrator can cancel another administrator\'s change',
      cancelAction: 'Cancel', stateActive: 'Active', stateExpired: 'Expired',
      saved: 'Change saved and applied',
      cancelled: 'Change cancelled, the time is released',
      needFields: 'Room, start and end time are required',
      failed: 'Failed to save', failedCancel: 'Failed to cancel',
      roomListFailed: 'Could not load the room list: {status}',
      sessionExpired: 'Your administrator session is no longer valid — sign in again.'
    },
    errors: {
      BAD_EMAIL: 'Use your HKU email address: it has to end in hku.hk, for example @connect.hku.hk',
      BAD_PASSWORD: 'Password must be at least 6 characters',
      BAD_USERNAME: 'Username must be at least 3 characters',
      USERNAME_TAKEN: 'That username is already taken',
      TIMETABLE_PRIORITY: 'The University timetable has priority: that time is taken by a class. '
        + 'You can only add or release time the timetable shows as free.',
      BAD_TIME: 'The start time must be on the hour and the end time must be at :50, for example '
        + '14:00 to 15:50.',
      FORBIDDEN: 'Only administrators can change room use time.',
      FORBIDDEN_OWNER: 'Only a super administrator can cancel another administrator\'s change.',
      UNAUTHORIZED: 'Your session is no longer valid — sign in again.',
      SERVER_ERROR: 'Something went wrong on the server.'
    }
  },

  'zh-CN': {
    app: { name: '空教室查询' },
    nav: {
      home: '首页', find: '查空教室', rooms: '浏览教室',
      manage: '修改教室使用时间', signOut: '退出登录', accounts: '账户管理'
    },
    lang: { label: '语言' },
    roles: {
      user: '普通用户（只浏览）', admin: '管理员', superadmin: '超级管理员'
    },
    accounts: {
      title: '账户管理', subtitle: '只有超级管理员能打开这个页面',
      roleHint: '角色是分层的：普通用户只能浏览；管理员还可以锁定或释放教室时间；'
        + '超级管理员还可以管理账户，包括提权和降权。',
      newAccount: '新建账户', username: '用户名', displayName: '显示名',
      email: 'HKU 邮箱', emailOptional: '可留空；填了必须是 hku.hk 后缀',
      role: '角色', actions: '操作', changeRole: '改角色',
      resetPassword: '重置密码', newPassword: '新密码',
      created: '账户已创建', roleChanged: '角色已更新', passwordChanged: '密码已更新',
      deleted: '账户已删除', loadFailed: '无法加载账户列表',
      saveFailed: '账户保存失败', deleteFailed: '账户删除失败'
    },
    common: {
      date: '日期', refresh: '刷新', save: '保存', cancel: '取消',
      edit: '编辑', delete: '删除', yes: '是', no: '否', none: '—', floor: '楼层'
    },
    login: {
      tagline: '看看课间哪几间教室空着',
      username: '用户名', usernameOrEmail: 'HKU 邮箱或用户名',
      email: 'HKU 邮箱，例如 name@connect.hku.hk', password: '密码',
      signIn: '登录', createAccount: '注册',
      hint: '注册只接受 HKU 邮箱：后缀必须是 hku.hk（@connect.hku.hk、@hku.hk 等）。'
        + '原型阶段不发验证邮件。',
      demo: '演示账号',
      createLink: '用 HKU 邮箱注册一个账号',
      backLink: '返回登录',
      signedIn: '已登录：{name}', created: '账号已创建，已登录：{name}',
      errSignIn: '登录失败', errSignUp: '注册失败'
    },
    find: {
      building: '楼栋', allBuildings: '全部楼栋',
      from: '从', to: '到', search: '查询', reset: '重置', sortBy: '排序',
      showAll: '显示全部教室',
      onlyFree: '只列出整段都空着的教室。',
      matched: '符合 {matched} / 共 {total} 间教室',
      listingMatching: '只列出符合条件的教室', listingEvery: '列出全部教室',
      freeStops: '空闲窗口在下一节课前 10 分钟结束，留出收拾离开的时间——所以可选到 :50。',
      colRoom: '教室', colLocation: '位置', colSeats: '座位', colType: '类型',
      colStatus: '状态', colWindow: '空闲窗口', colNext: '下节课',
      after: '{time} 之后', noWindowLong: '没有足够长的空档',
      empty: '没有教室在整段时间都空着。可以缩短时间、换楼栋，或打开「显示全部教室」。',
      noticePermission: '请注意：如果上课或教职员需要，你必须让出教室，教职员也可以请你离开。'
        + '本页只显示哪些教室空着，并不代表你有权使用。',
      noticeShared: '显示为空闲的教室可能已有其他同学在用，教室是共用的：「空闲」只表示没有被'
        + '课表或征用占住，不代表里面没人。',
      timelineTitle: '教室时间轴：{room}，{date}',
      legendInUse: '被占用（课或发布的变更，按整块）', legendFree: '空闲',
      sortWindow: '空闲窗口最长', sortCode: '房号 A→Z',
      sortBuilding: '按楼栋、再按房号', sortSeats: '座位最多优先',
      statusAvailable: '现在可用', statusLater: '稍后可用', statusInUse: '正在使用',
      statusNone: '没有空档',
      noTimelineHint: '点一行查看该教室当天的方格。'
    },
    rooms: {
      title: '所有教室', subtitle: '全部教室，不按时间过滤',
      filterPlaceholder: '按房号或楼栋筛选', noMatch: '没有符合筛选条件的教室。',
      roomLabel: '教室',
      attributesHint: '属性来自教室档案；课表由「课表 + 已发布的变更」计算得出',
      facilities: '设施', lastVerified: '最后核实 {when}',
      sampleData: '样例数据，尚未核实',
      seatsTotal: '座位总数', sockets: '插座', seatType: '座位类型',
      seatTypePlaceholder: '固定排座 / 长桌共用 / 独立桌椅',
      saveHint: '保存会把今天记为核实日期，并写入审计日志。',
      timetable: '当天课表',
      inUseTip: '被占用 {from} – {to}', freeTip: '空闲 {from} – {to}',
      noteAllFree: '这一天没有课、也没有发布的变更——全天可用。',
      noteInUse: '被占用 {ranges}',
      reviews: '学生评价', reviewCount: '{n} 条评价',
      ratingFrom: '★ {avg}，共 {n} 条评价',
      yourRating: '你的评分', yourReview: '你的评价',
      reviewPlaceholder: '在这里自习是什么体验？噪音、插座、座位、光线…',
      postReview: '发布评价', updateReview: '更新你的评价',
      deleteYours: '删除我的',
      confirmDeleteOthers: '确定删除这条评价吗？它是别的用户写的。',
      reviewHint: '一人一房一条评价。请评价这个房间，而不是里面的人。',
      newest: '最新优先', highest: '评分最高',
      noReviews: '还没有评价，来做第一个描述这间教室的人。',
      pickRoom: '在左侧选一间教室查看详情。',
      facilitiesSaved: '设施已保存，并盖上今天的核实日期',
      reviewSaved: '评价已保存', reviewDeleted: '评价已删除',
      needRating: '请给 1–5 星评分',
      needBody: '也请写几个字',
      reviewFailed: '评价保存失败', reviewDeleteFailed: '评价删除失败',
      reviewsFailed: '无法加载评价：{status}',
      facilitiesFailed: '设施保存失败',
      timelineFailed: '无法加载该教室的课表：{status}'
    },
    manage: {
      subtitle: '管理员可以添加或释放教室的使用时间。每次变更都会留审计，并会自己到期失效。',
      room: '教室', chooseRoom: '选择教室', whatHappens: '做什么',
      addUse: '添加使用', release: '释放时间',
      addUseHint: '把空闲时间占掉，例如教室要办活动。',
      releaseHint: '把时间还回来，例如某节课取消了。',
      date: '日期', fromHour: '从（整点）', toHour: '到（:50）',
      reason: '理由', reasonPlaceholder: '系里开会，约 25 人',
      expiresAt: '到期时间',
      expiresPlaceholder: '留空表示直到有人撤销才失效',
      submit: '提交变更',
      tabMine: '我的变更', tabAll: '所有管理员的变更', tabAudit: '审计日志',
      colChange: '变更', colInterval: '时间', colState: '状态', colAction: '操作',
      colSubmittedBy: '提交人', colWhen: '时间', colActor: '操作者', colDetail: '详情',
      allManagersHint: '这里列出所有管理员的变更，方便交接。你可以撤销自己提交的；要撤销别的管理员'
        + '提交的变更，需要超级管理员。数据不会被删除：撤销只是让它失效。',
      onlySuperCanCancel: '只有超级管理员能撤销别的管理员提交的变更',
      cancelAction: '撤销', stateActive: '生效中', stateExpired: '已失效',
      saved: '变更已保存并生效',
      cancelled: '变更已撤销，时间已释放',
      needFields: '请选择教室、开始与结束时间',
      failed: '保存失败', failedCancel: '撤销失败',
      roomListFailed: '无法加载教室列表：{status}',
      sessionExpired: '管理员登录态已失效——请重新登录。'
    },
    errors: {
      BAD_EMAIL: '请用 HKU 邮箱：后缀必须是 hku.hk，例如 @connect.hku.hk',
      BAD_PASSWORD: '密码至少 6 位',
      BAD_USERNAME: '用户名至少 3 个字符',
      USERNAME_TAKEN: '这个用户名已被占用',
      TIMETABLE_PRIORITY: '学校课表优先级最高：这段时间有课，只能在课表显示为空的时间上添加或释放。',
      BAD_TIME: '开始时间必须是整点，结束时间必须是 :50，例如 14:00 到 15:50。',
      FORBIDDEN: '只有管理员能修改教室使用时间。',
      FORBIDDEN_OWNER: '只有超级管理员能撤销别的管理员提交的变更。',
      UNAUTHORIZED: '登录态已失效——请重新登录。',
      SERVER_ERROR: '服务器出错了。'
    }
  },

  'zh-TW': {
    app: { name: '空教室查詢' },
    nav: {
      home: '首頁', find: '查空教室', rooms: '瀏覽教室',
      manage: '修改教室使用時間', signOut: '登出', accounts: '帳號管理'
    },
    lang: { label: '語言' },
    roles: {
      user: '一般使用者（僅瀏覽）', admin: '管理員', superadmin: '超級管理員'
    },
    accounts: {
      title: '帳號管理', subtitle: '只有超級管理員能打開這個頁面',
      roleHint: '角色是分層的：一般使用者只能瀏覽；管理員還可以鎖定或釋放教室時間；'
        + '超級管理員還可以管理帳號，包括提權與降權。',
      newAccount: '新增帳號', username: '使用者名稱', displayName: '顯示名稱',
      email: 'HKU 信箱', emailOptional: '可留空；填了必須是 hku.hk 後綴',
      role: '角色', actions: '操作', changeRole: '改角色',
      resetPassword: '重設密碼', newPassword: '新密碼',
      created: '帳號已建立', roleChanged: '角色已更新', passwordChanged: '密碼已更新',
      deleted: '帳號已刪除', loadFailed: '無法載入帳號列表',
      saveFailed: '帳號儲存失敗', deleteFailed: '帳號刪除失敗'
    },
    common: {
      date: '日期', refresh: '重新整理', save: '儲存', cancel: '取消',
      edit: '編輯', delete: '刪除', yes: '是', no: '否', none: '—', floor: '樓層'
    },
    login: {
      tagline: '看看課間哪幾間教室空著',
      username: '使用者名稱', usernameOrEmail: 'HKU 信箱或使用者名稱',
      email: 'HKU 信箱，例如 name@connect.hku.hk', password: '密碼',
      signIn: '登入', createAccount: '註冊',
      hint: '註冊只接受 HKU 信箱：後綴必須是 hku.hk（@connect.hku.hk、@hku.hk 等）。'
        + '原型階段不發驗證信。',
      demo: '示範帳號',
      createLink: '用 HKU 信箱註冊一個帳號',
      backLink: '返回登入',
      signedIn: '已登入：{name}', created: '帳號已建立，已登入：{name}',
      errSignIn: '登入失敗', errSignUp: '註冊失敗'
    },
    find: {
      building: '樓棟', allBuildings: '全部樓棟',
      from: '從', to: '到', search: '查詢', reset: '重設', sortBy: '排序',
      showAll: '顯示全部教室',
      onlyFree: '只列出整段都空著的教室。',
      matched: '符合 {matched} / 共 {total} 間教室',
      listingMatching: '只列出符合條件的教室', listingEvery: '列出全部教室',
      freeStops: '空檔在下一節課前 10 分鐘結束，留出收拾離開的時間——所以可選到 :50。',
      colRoom: '教室', colLocation: '位置', colSeats: '座位', colType: '類型',
      colStatus: '狀態', colWindow: '空檔', colNext: '下節課',
      after: '{time} 之後', noWindowLong: '沒有足夠長的空檔',
      empty: '沒有教室在整段時間都空著。可以縮短時間、換樓棟，或打開「顯示全部教室」。',
      noticePermission: '請注意：如果上課或教職員需要，你必須讓出教室，教職員也可以請你離開。'
        + '本頁只顯示哪些教室空著，並不代表你有權使用。',
      noticeShared: '顯示為空檔的教室可能已有其他同學在用，教室是共用的：「空檔」只表示沒有被'
        + '課表或徵用佔住，不代表裡面沒人。',
      timelineTitle: '教室時間軸：{room}，{date}',
      legendInUse: '被佔用（課或發布的變更，按整塊）', legendFree: '空檔',
      sortWindow: '空檔最長', sortCode: '房號 A→Z',
      sortBuilding: '按樓棟、再按房號', sortSeats: '座位最多優先',
      statusAvailable: '現在可用', statusLater: '稍後可用', statusInUse: '正在使用',
      statusNone: '沒有空檔',
      noTimelineHint: '點一列查看該教室當天的方格。'
    },
    rooms: {
      title: '所有教室', subtitle: '全部教室，不按時間過濾',
      filterPlaceholder: '按房號或樓棟篩選', noMatch: '沒有符合篩選條件的教室。',
      roomLabel: '教室',
      attributesHint: '屬性來自教室檔案；課表由「課表 + 已發布的變更」計算得出',
      facilities: '設施', lastVerified: '最後核實 {when}',
      sampleData: '樣本資料，尚未核實',
      seatsTotal: '座位總數', sockets: '插座', seatType: '座位類型',
      seatTypePlaceholder: '固定排座 / 長桌共用 / 獨立桌椅',
      saveHint: '儲存會把今天記為核實日期，並寫入稽核日誌。',
      timetable: '當天課表',
      inUseTip: '被佔用 {from} – {to}', freeTip: '空檔 {from} – {to}',
      noteAllFree: '這一天沒有課、也沒有發布的變更——全天可用。',
      noteInUse: '被佔用 {ranges}',
      reviews: '學生評價', reviewCount: '{n} 則評價',
      ratingFrom: '★ {avg}，共 {n} 則評價',
      yourRating: '你的評分', yourReview: '你的評價',
      reviewPlaceholder: '在這裡自習是什麼體驗？噪音、插座、座位、光線…',
      postReview: '發布評價', updateReview: '更新你的評價',
      deleteYours: '刪除我的',
      confirmDeleteOthers: '確定刪除這則評價嗎？它是其他使用者寫的。',
      reviewHint: '一人一房一則評價。請評價這個房間，而不是裡面的人。',
      newest: '最新優先', highest: '評分最高',
      noReviews: '還沒有評價，來做第一個描述這間教室的人。',
      pickRoom: '在左側選一間教室查看詳情。',
      facilitiesSaved: '設施已儲存，並蓋上今天的核實日期',
      reviewSaved: '評價已儲存', reviewDeleted: '評價已刪除',
      needRating: '請給 1–5 星評分',
      needBody: '也請寫幾個字',
      reviewFailed: '評價儲存失敗', reviewDeleteFailed: '評價刪除失敗',
      reviewsFailed: '無法載入評價：{status}',
      facilitiesFailed: '設施儲存失敗',
      timelineFailed: '無法載入該教室的課表：{status}'
    },
    manage: {
      subtitle: '管理員可以新增或釋放教室的使用時間。每次變更都會留稽核，並會自己到期失效。',
      room: '教室', chooseRoom: '選擇教室', whatHappens: '做什麼',
      addUse: '新增使用', release: '釋放時間',
      addUseHint: '把空檔佔掉，例如教室要辦活動。',
      releaseHint: '把時間還回來，例如某節課取消了。',
      date: '日期', fromHour: '從（整點）', toHour: '到（:50）',
      reason: '理由', reasonPlaceholder: '系上開會，約 25 人',
      expiresAt: '到期時間',
      expiresPlaceholder: '留空表示直到有人撤銷才失效',
      submit: '提交變更',
      tabMine: '我的變更', tabAll: '所有管理員的變更', tabAudit: '稽核日誌',
      colChange: '變更', colInterval: '時間', colState: '狀態', colAction: '操作',
      colSubmittedBy: '提交人', colWhen: '時間', colActor: '操作者', colDetail: '詳情',
      allManagersHint: '這裡列出所有管理員的變更，方便交接。你可以撤銷自己提交的；要撤銷別的管理員'
        + '提交的變更，需要超級管理員。資料不會被刪除：撤銷只是讓它失效。',
      onlySuperCanCancel: '只有超級管理員能撤銷別的管理員提交的變更',
      cancelAction: '撤銷', stateActive: '生效中', stateExpired: '已失效',
      saved: '變更已儲存並生效',
      cancelled: '變更已撤銷，時間已釋放',
      needFields: '請選擇教室、開始與結束時間',
      failed: '儲存失敗', failedCancel: '撤銷失敗',
      roomListFailed: '無法載入教室列表：{status}',
      sessionExpired: '管理員登入狀態已失效——請重新登入。'
    },
    errors: {
      BAD_EMAIL: '請用 HKU 信箱：後綴必須是 hku.hk，例如 @connect.hku.hk',
      BAD_PASSWORD: '密碼至少 6 位',
      BAD_USERNAME: '使用者名稱至少 3 個字元',
      USERNAME_TAKEN: '這個使用者名稱已被占用',
      TIMETABLE_PRIORITY: '學校課表優先級最高：這段時間有課，只能在課表顯示為空的時間上新增或釋放。',
      BAD_TIME: '開始時間必須是整點，結束時間必須是 :50，例如 14:00 到 15:50。',
      FORBIDDEN: '只有管理員能修改教室使用時間。',
      FORBIDDEN_OWNER: '只有超級管理員能撤銷別的管理員提交的變更。',
      UNAUTHORIZED: '登入狀態已失效——請重新登入。',
      SERVER_ERROR: '伺服器出錯了。'
    }
  }
}
