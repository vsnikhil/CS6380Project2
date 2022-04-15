public enum Type {
    BRD, //broadcast ( from the root )
    ACK, //ack message ( if sender is parent )
    REJ, //reject message ( if sender is not parent )
    CVG, //broadcast leader to all follower ( if flood finished )
    BGN, //begin one round ( sent by master to worker for begin of one round )
    END, //end one round ( sent by worker to master for report end of a round )
    TMN, //tell other process current process terminated ( confirm message to terminate the flood )
    FIN, //kill all process ( sent by master to worker )
}
