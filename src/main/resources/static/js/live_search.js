document.querySelector('#input').oninput = function () {
    let value = this.value.trim().toLowerCase();
    let list = document.querySelectorAll('.test');
    if(value!=''){
        list.forEach(function (element){
            if(element.querySelector('.title').innerText.toLowerCase().search(value)==-1 && element.querySelector('.desc').innerText.toLowerCase().search(value)==-1 ){
                element.style.display = "none";
            }else{
                element.style.display="";
            }
        });
    } else {
        list.forEach(function (element){
            element.style.display="";
        });
    }
}