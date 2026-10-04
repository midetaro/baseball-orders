    // 左メニューの開閉。他の画面への導線はメニュー内にだけ置き、既定では閉じておく。
    const menuToggle = document.querySelector<HTMLButtonElement>('#menu-toggle')!, siteMenu = document.querySelector<HTMLElement>('#site-menu')!, menuClose = document.querySelector<HTMLButtonElement>('#menu-close')!, menuBackdrop = document.querySelector<HTMLElement>('#menu-backdrop')!;
    function setMenuOpen(open: boolean): void { siteMenu.hidden=!open; menuBackdrop.hidden=!open; menuToggle.setAttribute('aria-expanded',String(open)); if (open) { siteMenu.querySelector<HTMLAnchorElement>('a')!.focus(); } else { menuToggle.focus(); } }
    menuToggle.addEventListener('click',()=>setMenuOpen(Boolean(siteMenu.hidden)));
    menuClose.addEventListener('click',()=>setMenuOpen(false));
    menuBackdrop.addEventListener('click',()=>setMenuOpen(false));
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!siteMenu.hidden)setMenuOpen(false);});
