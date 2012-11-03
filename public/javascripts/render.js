$(function () {
    var onlinePrefix = "$DRAWSHIT$online$";
    var loc = window.location, new_uri;
    if (loc.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    new_uri += "//" + loc.host;
    new_uri += "/ws/" + window.chatroomName;
    setTimeout(function(){
    var connection = new WebSocket(new_uri);
    connection.binaryType = "arraybuffer";
    connection.onopen = function(){
        setInterval(function(){connection.send("ping")},5000);
        connection.send(onlinePrefix);
    }
    connection.onmessage = function (e) {
        var data = e.data;
        if(typeof data === "string"){
            console.log(data)
            if(data.indexOf(onlinePrefix) > -1){
                $("#onlineCount").text(data.substring(onlinePrefix.length))
            }
        }else {
            var canvasDiv = $('#drawins');
            var canvas = $("<canvas>");
            canvas.prependTo(canvasDiv);
            canvas.attr('width', 300);
            canvas.attr('height', 300);
            canvas.css('float', 'left');
            var context = canvas[0].getContext("2d");
            var buf = context.createImageData(300, 300);
            var ints = buf.data;

            var input = new Uint8Array(data);
            var pushWhite = function (index) {
                ints[index * 4] = 255;
                ints[index * 4 + 1] = 255;
                ints[index * 4 + 2] = 255;
                ints[index * 4 + 3] = 255;
            };
            var pushBlack = function (index) {
                ints[index * 4] = 0;
                ints[index * 4 + 1] = 0;
                ints[index * 4 + 2] = 0;
                ints[index * 4 + 3] = 255;
            };

            var b;
            for (var i = 0; i < input.length; i++) {
                b = input[i];
                if (b & 1) {
                    pushBlack(i * 8)
                } else {
                    pushWhite(i * 8)
                }
                if (b & 2) {
                    pushBlack(i * 8 + 1)
                } else {
                    pushWhite(i * 8 + 1)
                }
                if (b & 4) {
                    pushBlack(i * 8 + 2)
                } else {
                    pushWhite(i * 8 + 2)
                }
                if (b & 8) {
                    pushBlack(i * 8 + 3)
                } else {
                    pushWhite(i * 8 + 3)
                }
                if (b & 16) {
                    pushBlack(i * 8 + 4)
                } else {
                    pushWhite(i * 8 + 4)
                }
                if (b & 32) {
                    pushBlack(i * 8 + 5)
                } else {
                    pushWhite(i * 8 + 5)
                }
                if (b & 64) {
                    pushBlack(i * 8 + 6)
                } else {
                    pushWhite(i * 8 + 6)
                }
                if (b & 128) {
                    pushBlack(i * 8 + 7)
                } else {
                    pushWhite(i * 8 + 7)
                }
            }

            canvas.width = canvas.width;
            context.putImageData(buf, 0, 0);
            canvas.click(function(){
                var pad = $("#canvas")[0];
                pad.getContext("2d").putImageData(context.getImageData(0,0,300,300),0,0);
            })
        }

    }
    var canvas = $("#canvas")[0];
    var context = canvas.getContext("2d");

    context.fillStyle = 'rgb(255, 255, 255)';
    context.fillRect(0, 0, canvas.width, canvas.height);

    var lastX = -20;
    var lastY = -20;
    var mouseDown = false;
    var lines = new Array();
    var paint;

    function addClick(x, y, dragging) {
        if (dragging && lastX > 0 && lastY > 0) {
            lines.push([lastX, lastY, x, y]);
        } else if (!dragging && lastX > 0 && lastY > 0) {
            lines.push([lastX - 1, lastY, lastX, lastY]);
        }
        lastX = x;
        lastY = y;
    }


    function redraw() {

        context.strokeStyle = "#000000";
        context.lineJoin = "round";
        context.lineWidth = 5;

        for (var i = 0; i < lines.length; i++) {
            var line = lines.shift();
            context.beginPath();
            context.moveTo(line[0], line[1]);
            context.lineTo(line[2], line[3]);
            context.closePath();
            context.stroke();
        }
    }

    $('#canvas').mousedown(function (e) {
        e.preventDefault();
        e.stopPropagation();

        // you can change the cursor if you want
        // just remember to handle the mouse up and put it back :)
        e.target.style.pointer = 'move';
        var mouseX = e.pageX - this.offsetLeft - 11;
        var mouseY = e.pageY - this.offsetTop - 11;
        paint = true;
        addClick(mouseX, mouseY);
        redraw();
    });

    $('#canvas').mousemove(function (e) {
        if (paint) {
            var mouseX = e.pageX - this.offsetLeft - 11;
            var mouseY = e.pageY - this.offsetTop - 11;
            addClick(mouseX, mouseY, true);
            redraw();
        }
    });

    $('#canvas').mouseup(function (e) {
        paint = false;
        lines.push([lastX - 1, lastY, lastX, lastY]);
        lastX = -1;
        lastY = -1;
        redraw();
    });

    $('#canvas').mouseleave(function (e) {
        paint = false;
    });

    $('#canvas').onselectstart = function () { return false; };
    $('#canvas').onmousedown = function () { return false; };

    $("#send").click(function () {
        var im = context.getImageData(0, 0, canvas.width, canvas.height);
        var data = im.data;
        var buffer = new ArrayBuffer(data.length / 4 / 8);
        var ints = new Uint8Array(buffer);
        var p1, p2, p3, p4, p5, p6, p7, p8;
        for (var i = 0; i < data.length; i = i + 32) {
            p1 = data[i] != 255 ? 1 : 0;
            p2 = data[i + 4]!= 255 ? 2 : 0;
            p3 = data[i + 2 * 4] != 255 ? 4 : 0;
            p4 = data[i + 3 * 4] != 255 ? 8 : 0;
            p5 = data[i + 4 * 4] != 255 ? 16 : 0;
            p6 = data[i + 5 * 4] != 255 ? 32 : 0;
            p7 = data[i + 6 * 4] != 255 ? 64 : 0;
            p8 = data[i + 7 * 4] != 255 ? 128 : 0;
            ints[i / 32] = p1 | p2 | p3 | p4 | p5 | p6 | p7 | p8;
        }
        window.last = data;
        connection.send(buffer);
    })

    $("#clear").click(function () {
        context.fillRect(0, 0, canvas.width, canvas.height);
    })
    },1000)
})