// Mondrian properties for Cube addon

(function(AJS,$){
    $(function(){
        $('#cube-jdbc-settings').each(function(){
            var $form = $(this);
            AJS.$.ajax({
                url: AJS.contextPath() + '/rest/cube/1.0/jdbc',
                dataType: 'json'
            }).done(function(props){
                for (var key in props){
                    $form.find('input[name="' + key.substring(5) + '"]').val(props[key]);
                }
            }).fail(function(jqxhr){
                console.error('com.mesilat.cube', jqxhr.responseText);
            });

            $form.submit(function(e){
                e.preventDefault();
                var data = {
                    'jdbc.url': $form.find('input[name="url"]').val(),
                    'jdbc.username': $form.find('input[name="username"]').val(),
                    'jdbc.password': $form.find('input[name="password"]').val(),
                    'jdbc.driver': $form.find('input[name="driver"]').val()
                };
                AJS.$.ajax({
                    url: AJS.contextPath() + '/rest/cube/1.0/jdbc',
                    type: 'PUT',
                    contentType: 'application/json',
                    data: JSON.stringify(data),
                    processData: false,
                    dataType: 'text'
                }).done(function(msg){
                    alert(msg);
                }).fail(function(jqxhr){
                    alert(jqxhr.responseText);
                });
            });
        });
        
        $('#mondrian-properties').each(function(){
            //console.log('com.mesilat.cube', 'Properties page...');
            AJS.$.ajax({
                url: AJS.contextPath() + '/rest/cube/1.0/properties',
                dataType: 'json',
                context: $(this)
            }).done(function(props){
                //console.log('com.mesilat.cube', props);
                var $root = this;
                $root.empty();
                var $dlg = $(Mesilat.Cube.Templates.propertiesDialog({
                    properties: props
                }));
                $dlg.appendTo($root);
                $dlg.find('.com-mesilat-mondrian-property-addrow').each(function(){
                    $(this).on('click',function(e){
                        console.log('com.mesilat.cube', 'Add row');
                        var $line = $(Mesilat.Cube.Templates.propertyLine({
                            name: '',
                            value: ''
                        }));
                        $line.find('.com-mesilat-mondrian-property-remove').each(function(){
                            $(this).on('click', function(e){
                                e.preventDefault();
                                $(e.target).closest('tr').remove();
                            });
                        });
                        $root.find('tbody').append($line);
                    });
                });
                $dlg.find('.com-mesilat-mondrian-property-save').each(function(){
                    $(this).on('click', function(e){
                        var data = {};
                        $dlg.find('tr').each(function(){
                            var name = $(this).find('.com-mesilat-mondrian-property-name').val();
                            var value = $(this).find('.com-mesilat-mondrian-property-value').val();
                            if (name !== ''){
                                data[name] = value;
                            }
                        });
                        AJS.$.ajax({
                            url: AJS.contextPath() + '/rest/cube/1.0/properties',
                            type: 'PUT',
                            contentType: 'application/json',
                            data: JSON.stringify(data),
                            processData: false,
                            dataType: 'text'
                        }).done(function(msg){
                            alert(msg);
                        }).fail(function(jqxhr){
                            alert(jqxhr.responseText);
                        });
                    });
                });
                $dlg.find('.com-mesilat-mondrian-property-remove').each(function(){
                    $(this).on('click', function(e){
                        e.preventDefault();
                        $(e.target).closest('tr').remove();
                    });
                });
            }).fail(function(jqxhr){
                console.error('com.mesilat.cube', jqxhr.responseText);
            });
        });
    });

    function modifyPluginDescription(){
        $('div.upm-plugin[@data-key="com.mesilat.cube"] div.upm-details:visible').each(function(){
            var $details = $(this);
            if (!$details.hasClass('com-mesilat-description-updated')){
                $details.addClass('com-mesilat-description-updated');
                $details.find('p.upm-plugin-summary').append($('<div>. For usage details please refer to <a href="#">documentation page</a></div>').html());
            }
        });
        setTimeout(modifyPluginDescription, 1000);
    }

    $(function(){
        setTimeout(modifyPluginDescription, 1000);
    });

})(AJS,AJS.$||$);