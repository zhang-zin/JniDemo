#ifndef JNIDEMO_SAFE_QUEUE_H
#define JNIDEMO_SAFE_QUEUE_H

#include <queue>
#include <pthread.h>

using namespace std;

template<typename T>
class SafeQueue {
private:
    typedef void (*ReleaseCallback)(T *); //回调 用来释放T的内容
    typedef void (*SyncCallback)(SafeQueue<T> *); //

private:
    queue<T> queue;
    pthread_mutex_t mutex; //互斥锁
    pthread_cond_t cond; // 等待和唤醒
    int work; //标记队列是否工作
    ReleaseCallback releaseCallback;
    SyncCallback syncCallback;

public:
    SafeQueue() {
        pthread_mutex_init(&mutex, 0); //初始化互斥锁
        pthread_cond_init(&cond, 0);   //初始化条件变量
    }

    ~SafeQueue() {
        pthread_mutex_destroy(&mutex);
        pthread_cond_destroy(&cond);
    }

    void setReleaseCallback(ReleaseCallback releaseCallback) {
        this->releaseCallback = releaseCallback;
    }

    void setSyncCallback(SyncCallback syncCallback) {
        this->syncCallback = syncCallback;
    }

    void sync() {
        pthread_mutex_lock(&mutex);
        syncCallback(this);
        pthread_mutex_unlock(&mutex);
    }

    /**
     * 入队，【AVPacket 压缩包】 【AVFrame 原始包】
     * @param value
     */
    void insertToQueue(T value) {
        pthread_mutex_lock(&mutex); //多线程操作，先锁住

        if (work) {
            queue.push(value);
            pthread_cond_signal(&cond); //当插入数据包时，通知唤醒
        } else {
            if (releaseCallback) {
                releaseCallback(&value);  //让外部释放 value
            }
        }

        pthread_mutex_unlock(&mutex); //解锁
    }

    /**
     * 出队
     * @param value
     */
    int getQueueAndDel(T &value) {
        int ret = 0;
        pthread_mutex_lock(&mutex);

        while (work && queue.empty()) {
            pthread_cond_wait(&cond, &mutex); //等待唤醒
        }

        if (!queue.empty()) {
            //取出队列的数据包，并删除
            value = queue.front();
            queue.pop();
            ret = 1;
        }

        pthread_mutex_unlock(&mutex);

        return ret;
    }

    void setWork(int work) {
        pthread_mutex_lock(&mutex);
        this->work = work;

        pthread_cond_signal(&cond);

        pthread_mutex_unlock(&mutex);
    }

    int empty() {
        return queue.empty();
    }

    int size() {
        return queue.size();
    }

    void clear() {
        pthread_mutex_lock(&mutex); // 多线程的访问（先锁住）

        unsigned int size = queue.size();

        for (int i = 0; i < size; ++i) {
            //循环释放队列中的数据
            T value = queue.front();
            if (releaseCallback) {
                releaseCallback(&value); // 让外界去释放堆区空间
            }
            queue.pop(); // 删除队列中的数据，让队列为0
        }

        pthread_mutex_unlock(&mutex); // 多线程的访问（要解锁）
    }

};


#endif //JNIDEMO_SAFE_QUEUE_H
