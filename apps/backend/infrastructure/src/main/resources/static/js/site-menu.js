    // 左メニューの開閉。他の画面への導線はメニュー内にだけ置き、既定では閉じておく。
    const menuToggle = document.querySelector('#menu-toggle'), siteMenu = document.querySelector('#site-menu'), menuClose = document.querySelector('#menu-close'), menuBackdrop = document.querySelector('#menu-backdrop');
    function setMenuOpen(open) { siteMenu.hidden=!open; menuBackdrop.hidden=!open; menuToggle.setAttribute('aria-expanded',String(open)); if (open) { siteMenu.querySelector('a').focus(); } else { menuToggle.focus(); } }
    menuToggle.addEventListener('click',()=>setMenuOpen(siteMenu.hidden));
    menuClose.addEventListener('click',()=>setMenuOpen(false));
    menuBackdrop.addEventListener('click',()=>setMenuOpen(false));
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!siteMenu.hidden)setMenuOpen(false);});
