export async function api(path,options={}){
 let response;
 try{response=await fetch(path,{...options,credentials:'same-origin',headers:{...(options.body?{'Content-Type':'application/json'}:{}),...options.headers}});}
 catch{throw new Error('تعذر الاتصال بالخادم.');}
 const data=await response.json().catch(()=>({}));
 if(!response.ok){const error=new Error(data.error||'تعذر إكمال الطلب.');error.status=response.status;error.details=data;throw error;}
 return data;
}
