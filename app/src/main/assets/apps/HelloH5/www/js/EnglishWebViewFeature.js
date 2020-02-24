document.addEventListener('plusready',function () {   //固定模板,不用修改

        var _BARCODE ='EnglishWebView' // ① 插件名称(很重要),变量名和字符串值可自定义
        var bridge = window.plus.bridge //②固定模板,不用修改,
        var EnglishWebViewPlugin = { //③变量名可自定义
            //该模板做了简单的封装,因为需要调用的逻辑比较简单,而且参数一致,该方法中的参数根据自己需求修改
            callNative:function (fname, args, successCallback) { 
                var callbackId =this.getCallbackId(successCallback,this.errorCallback) //固定模板,不用修改
                //该if条件是对参数的判断
                if (args !=null) {
                    args.unshift(callbackId)
                }else {
                    var args = [callbackId]
                }
                return bridge.exec(_BARCODE, fname, args)  //一个很重要的方法,第一个参数_BARCODE是插件名,同注释①的变量名一致;第二个参数fname是js方法名,第三个参数是要传递的参数;
            },
            //模板回调方法,可不用修改
            getCallbackId:function (successCallback) {
                var success =typeof successCallback !=='function' ?null :function (args) {
                    successCallback(args)
                }
                return bridge.callbackId(success,this.errorCallback)
            },
            //模板回调方法,可不用修改
            errorCallback:function (errorMsg) {
                console.log('Javascript callback error: ' + errorMsg)
            },
            //自定义js方法,但是必须要和扩展类的的方法名称一致,否则无法调用原生api
            startEnglishWebView:function (argus1, argus2) {
                this.callNative('startEnglishWebView', [argus1, argus2], null)  //第一个参数方法名称字符串,必须和方法名一致,参数二是要传递的参数
            }
        };
        window.plus.EnglishWebView = EnglishWebViewPlugin //固定模板,示例:window.plus.插件名 = 同注释③变量名一致
},true);