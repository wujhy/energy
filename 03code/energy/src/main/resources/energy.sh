#!/bin/bash
# ./ry.sh start 启动 stop 停止 restart 重启 status 状态
# 应用名
APP_NAME=shanhe-energy-single.jar
# 应用端口号
APP_PORT=8081
# 应用根路径
APP_PATH=/opt/energy/
# 备份路径
BACKUP_PATH=/opt/energy/files/backup/
# 远程项目下载路径
DEPLOY_PATH=/opt/energy/files/softDownload/
# jdk安装路径
JDK_PATH=/usr/local/jdk1.8.0_431
# JVM参数
JVM_OPTS="-Duser.timezone=Asia/Shanghai -Xmn200m -Xms200m -Xmx1g -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=2 -XX:ParallelGCThreads=2 -XX:ConcGCThreads=1 -Dspring.main.lazy-initialization=true -Dspring.jmx.enabled=false -Dspring.devtools.restart.enabled=false -Dspring.main.web-application-type=servlet"
# 应用参数
APP_OPTS="-Dlogback.loglevel=INFO --spring.profiles.active=prod --server.port=${APP_PORT}"

# 启动项目，失败则判断是否还原
function startOrRevert() {
  start
  startStatus
  # 启动结果失败
  if [ $? -eq 1 ]; then
    # 检查是否存在当天的备份文件
    TIMESTAMP=$(date +%Y-%m-%d)
    BACKUP_FILE=$(find ${BACKUP_PATH} -type f -name "${APP_NAME}.${TIMESTAMP}*")
    if [ -n "$BACKUP_FILE" ]; then
      # 还原项目
      revert
      # 继续
      start
      # 删除备份
      backupDelete
    fi
  fi
}

# 启动项目
function start() {
    # 检查服务是否启动
    PID=$(netstat -nlp | grep ${APP_PORT} | awk '{print $7}' |cut -d/ -f 1)
    # -n：no zero，-z：zero
    if [ -n "$PID" ]; then
        echo "[${APP_PORT}] 端口被 $PID 绑定"
        # 端口被绑定，先执行关闭
        stop
        sleep 2
    fi

    echo "[${APP_PATH}${APP_NAME}] 启动 $(date +%Y-%m-%d_%H:%M:%S)..."

    cd ${APP_PATH} || exit
    echo "nohup ${JDK_PATH}/bin/java ${JVM_OPTS} -jar ${APP_PATH}${APP_NAME} ${APP_OPTS} > /dev/null 2>&1 &"
    nohup ${JDK_PATH}/bin/java ${JVM_OPTS} -jar ${APP_PATH}${APP_NAME} ${APP_OPTS} > /dev/null 2>&1 &
}

# 监听启动状态
function startStatus() {
    # 循环监控
    for st in $(seq 1 60)
    do
        # 监听端口号
        PID=$(netstat -nlp | grep ${APP_PORT} | awk '{print $7}' |cut -d/ -f 1)
        if [ -z "$PID" ]; then
            # 继续循环等待
            sleep 3
            echo -e ".\c"
        else
            echo ""
            echo "[${APP_PATH}${APP_NAME}] 启动成功，端口号:${APP_PORT}, 进程pid：$PID, 耗时：$(($((st-1))*3)) seconds！！！"
            return 0
        fi
    done
    # 循环结束，服务启动失败
    echo "监听时间内启动失败！！！"
    return 1
}

# 停止项目
function stop() {
    echo "[${APP_PATH}${APP_NAME}] 关闭 $(date +%Y-%m-%d_%H:%M:%S)..."
    # 关闭进程
    pid=$(ps -ef | grep ${APP_PATH}${APP_NAME} | grep -v grep | awk '{print $2}')
    if [ -n "$pid" ]; then
        closeWatchDog
        # 正常关闭
        echo "[${APP_PATH}${APP_NAME}] kill -15 [$pid]"
        kill -15 $pid
        seconds=1
        while true; do
            pid=$(ps -ef | grep ${APP_PATH}${APP_NAME} | grep -v grep | awk '{print $2}')
            if [ -z "$pid" ]; then
                break
            fi
            echo -e ".\c"
            seconds=$((seconds + 1))
            # 超过 60 秒强制关闭
            if [ $seconds -gt 60 ]; then
                kill -9 $pid
            fi
            # 每10秒关闭一次
            if [ $((seconds % 10)) -eq 0 ]; then
              kill -15 $pid
            fi
            sleep 1
        done
        echo "关闭进程完成，耗时：${seconds} seconds"
    else
        # 服务未启动无需关闭
        echo "[${APP_PATH}${APP_NAME}] 进程未启动，无需关闭"
    fi

    # 关闭端口号
    PID=$(netstat -nlp | grep ${APP_PORT} | awk '{print $7}' |cut -d/ -f 1)
    if [ -n "$PID" ]; then
        closeWatchDog
        # 正常关闭
        echo "[${APP_PORT}] kill -15 [$PID]"
        kill -15 "$PID"
        # 关闭等待 60 秒
        for ((i = 0; i < 20; i++))
        do
            sleep 3
            PID=$(netstat -nlp | grep ${APP_PORT} | awk '{print $7}' |cut -d/ -f 1)
            if [ -n "$PID" ]; then
                echo -e ".\c"
            else
                echo ""
                echo "[${APP_PORT}] 关闭成功！！！"
                break
            fi
        done
        # 正常关闭失败，强制关闭
        if [ -n "$PID" ]; then
            echo "监听时间内关闭失败！！！"
            echo "[${APP_PORT}] 强制关闭 kill -9 [$PID]"
            kill -9 "$PID"
        fi
    else
        # 服务未启动无需关闭
        echo "[${APP_PORT}] 端口未占用，无需关闭！！！"
    fi
    echo "[${APP_PATH}${APP_NAME}] 关闭完成！！！"
}

# 重启
function restart() {
    stop
    sleep 2
    start
}

# 校验项目运行状态
function status() {
    # 打印第二个参数pid， （wc -l 满足查询条数）
    pid=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
    if [ -z "${pid}" ]; then
        echo "[${APP_PATH}${APP_NAME}] 未启动！！！"
        return 1
    else
        echo "[${APP_PATH}${APP_NAME}] 启动中. PID: [${pid}]！！！"
        return 0
    fi
}

# 提示信息
function usage() {
    echo "Usage: ./energy.sh [start | stop | restart | status | backup | revert]"
    exit 1
}

# 安装 source /opt/energy/energy.sh jdk
function jdk() {
    # 目录存在则不安装
    if [ -d "$JDK_PATH" ]; then
        echo "${JDK_PATH} 已经安装，无需重复执行！！！"
    else
      # 解压
      echo "解压：tar -xzvf ${APP_PATH}jdk-8u431-linux-aarch64.tar.gz -C /usr/local"
      tar -xzvf ${APP_PATH}jdk-8u431-linux-aarch64.tar.gz -C /usr/local
      sleep 1
      # 修改环境变量，文件末尾追加
      echo "# JAVA_HOME" >> /etc/profile
      echo "export JAVA_HOME=${JDK_PATH}" >> /etc/profile
      echo "export JRE_HOME=${JDK_PATH}/jre" >> /etc/profile
      echo "export PATH=${PATH}:${JDK_PATH}/bin:${JDK_PATH}/jre/bin" >> /etc/profile
      echo "export CLASSPATH=.:${JDK_PATH}/lib/tools.jar:${JDK_PATH}/lib/dt.jar:${JDK_PATH}/lib:${JDK_PATH}/jre/lib" >> /etc/profile
      # 生效
      source /etc/profile
      # 自行生效或验证
      echo "jdk安装成功 | java -version | whereis java"
    fi
}

# 启动浏览器
function chromium() {
# 定义要检查的网站
website="http://127.0.0.1:${APP_PORT}"
# 循环5次检查网站
for i in {1..5}
do
  # 使用curl检查网站，-f表示失败时返回非0状态码，-s表示静默模式，不输出错误信息
  if curl -f -s --head $website; then
      export DISPLAY=:0
      xhost +
      #echo "全屏启动浏览器：su - shanhe -c chromium --start-fullscreen --kiosk http://127.0.0.1:${APP_PORT}/"
      su - shanhe -c 'chromium --start-fullscreen --no-first-run --disable-features=Translate --disable-extensions --lang=zh-CN --disable-pinch --kiosk '${website}'/'
      break
  else
      sleep 1  # 每次检查后暂停1秒
  fi
done
}

#重启浏览器
function restartChromium(){
  pkill chromium-bin
  sleep 1
  clearMemory
  sleep 2
  chromium
}

#清除缓存，只清除页面缓存即可
function clearMemory(){
  sudo sync && echo 1 | sudo tee /proc/sys/vm/drop_caches
}

function closeWatchDog() {
  echo "关闭看门狗： echo 0 >/sys/class/gzpeite/user/watch_dog"
  echo 0 >/sys/class/gzpeite/user/watch_dog
}

# 安装 source /opt/energy/energy.sh service
function service() {
    # 拷贝文件
    cp "${APP_PATH}energy.service" "/etc/systemd/system/energy.service"
    # 重新加载systemd配置
    systemctl daemon-reload
    systemctl enable energy.service
    systemctl start energy.service
    echo "systemctl enable|start|stop|restart|status energy.service"
}

# 还原最新jar包
function revert() {
    echo "[${APP_PATH}${APP_NAME}] 还原 $(date +%Y-%m-%d_%H:%M:%S)..."
    # 备份文件夹是否存在
    if [ ! -d "${BACKUP_PATH}" ]; then
        echo "备份目录 [${BACKUP_PATH}] 不存在，无法执行还原"
        exit 1
    fi

    LATEST_TIMESTAMP=""
    LATEST_FILE=""
    for FILE in "${BACKUP_PATH}"*; do
        if [[ "$FILE" == "${BACKUP_PATH}${APP_NAME}"* ]]; then
            TIMESTAMP=${FILE: -25:24}
            TIMESTAMP=${TIMESTAMP#*.}
            if [[ "$TIMESTAMP" > "$LATEST_TIMESTAMP" ]] || [ -z "$LATEST_TIMESTAMP" ]; then
                LATEST_TIMESTAMP="$TIMESTAMP"
                LATEST_FILE="$FILE"
            fi
        fi
    done

    if [ -z "$LATEST_FILE" ]; then
        echo "备份目录 [${BACKUP_PATH}] 未找到 [${APP_NAME}]"
        exit 1
    fi

    echo "[${APP_PATH}${APP_NAME}] 还原自 [${LATEST_FILE}]"
    cp "${LATEST_FILE}" "${APP_PATH}${APP_NAME}"
    echo "[${APP_PATH}${APP_NAME}] 还原完成！！！"
}

# 备份处理
function backup() {
    echo "[${APP_PATH}${APP_NAME}] 备份 $(date +%Y-%m-%d_%H:%M:%S)..."
    # 创建目录
    mkdir -p ${BACKUP_PATH}
    TIMESTAMP=$(date +%Y-%m-%d_%H:%M:%S)
    # 拷贝文件
    echo "[${APP_PATH}${APP_NAME}] 备份至 [${BACKUP_PATH}${APP_NAME}.${TIMESTAMP}]"
    cp "${APP_PATH}${APP_NAME}" "${BACKUP_PATH}${APP_NAME}.${TIMESTAMP}"
    echo "[${APP_PATH}${APP_NAME}] 备份完成！！！"
}

# 备份删除
function backupDelete() {
    echo "[${APP_PATH}${APP_NAME}] 备份删除 $(date +%Y-%m-%d_%H:%M:%S)..."

    # 获取文件夹中的所有文件列表
    files=$(ls "${BACKUP_PATH}")

    # 将文件列表转换为数组
    file_array=($files)

    # 获取文件夹中文件的数量
    file_count=${#file_array[@]}

    # 判断文件数量是否大于1，如果是则执行删除操作
    if [ "$file_count" -gt 1 ]; then
        # 遍历文件数组，从第二个文件开始删除，直到倒数第二个文件
        for ((i=0; i<$((file_count-1)); i++)); do
            echo "rm ${BACKUP_PATH}${file_array[$i]}"
            rm "${BACKUP_PATH}${file_array[$i]}"
        done
    fi
    echo "[${APP_PATH}${APP_NAME}] 备份删除完成！！！"
}

# 迁移升级包
function transfer() {
    echo "[${APP_PATH}${APP_NAME}] 迁移升级包 $(date +%Y-%m-%d_%H:%M:%S)..."

    # 检查部署目录是否存在，移除
    if [ ! -s "${APP_PATH}${APP_NAME}" ]; then
        echo "[${APP_PATH}${APP_NAME}] 旧包不存在，无需删除"
    else
        echo "[${APP_PATH}${APP_NAME}] 删除旧包完成"
        rm -rf ${APP_PATH}${APP_NAME}
    fi

    # 拷贝项目
    echo "[${APP_PATH}${APP_NAME}] 迁移自 [${DEPLOY_PATH}${APP_NAME}]"
    cp "${DEPLOY_PATH}${APP_NAME}" "${APP_PATH}${APP_NAME}"
    echo "[${APP_PATH}${APP_NAME}] 迁移升级包完成！！！"
}

# 升级部署
function deploy() {
    # 备份当前项目
    backup
    # 迁移升级包
    transfer
    # 停止项目
    stop
}

# 升级部署
function deploy1() {
    # 停止项目stop
    #systemctl stop energy.service
    stop
    # 备份当前项目
    backup
    # 迁移升级包
    transfer
    # 启动项目start
    systemctl start energy.service
    # 监听启动状态
    startStatus
    # 启动结果失败
    if [ $? -eq 1 ]; then
      # 停止项目stop
      #systemctl stop energy.service
      stop
      # 还原项目
      revert
      # 启动项目start
      systemctl start energy.service
    fi
    # 删除备份
    backupDelete
}

case "$1" in
    "status")
        status;;
    "start")
        start;;
    "startOrRevert")
        startOrRevert;;
    "startStatus")
        startStatus;;
    "stop")
        stop;;
    "restart")
        restart;;
    "service")
        service;;
    "jdk")
        jdk;;
    "backup")
        backup;;
    "backupDelete")
        backupDelete;;
    "transfer")
        transfer;;
    "revert")
        revert;;
    "deploy")
        deploy;;
    "chromium")
        chromium;;
    "restartChromium")
        restartChromium;;
    "closeWatchDog")
        closeWatchDog;;
    *)
        usage;;
esac