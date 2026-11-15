export const mediaExtensions = Object.freeze({
 'image/jpeg':'jpg','image/png':'png','image/webp':'webp','image/gif':'gif','video/mp4':'mp4','video/webm':'webm',
 'audio/mpeg':'mp3','audio/mp4':'m4a','audio/ogg':'ogg','application/pdf':'pdf'
});
export function contentTypesForPath(path) {
 const ext=path.toLowerCase().split('.').pop();
 return Object.entries(mediaExtensions).filter(([type,suffix])=>(suffix===ext || (ext==='jpeg'&&suffix==='jpg')) &&
   (path.startsWith('media/') || type.startsWith('image/'))).map(([type])=>type);
}
